import type {
  CardGame,
  CatalogCard,
  CatalogCardDetails,
  PokemonAttack,
} from '@/types/Catalog'

export const GAME_INFO: Record<CardGame, { slug: string; name: string }> = {
  POKEMON: { slug: 'pokemon', name: 'Pokémon' },
  YUGIOH: { slug: 'yugioh', name: 'Yu-Gi-Oh!' },
  MAGIC_THE_GATHERING: { slug: 'magic', name: 'Magic: The Gathering' },
}

export const CARD_TYPE_LABELS: Record<string, string> = {
  POKEMON: 'Pokémon',
  TRAINER: 'Treinador',
  ENERGY: 'Energia',
  MONSTER: 'Monstro',
  SPELL: 'Magia',
  TRAP: 'Armadilha',
  LAND: 'Terreno',
  CREATURE: 'Criatura',
  ARTIFACT: 'Artefato',
  ENCHANTMENT: 'Encantamento',
  PLANESWALKER: 'Planeswalker',
  BATTLE: 'Batalha',
  INSTANT: 'Mágica instantânea',
  SORCERY: 'Feitiço',
  KINDRED: 'Kindred',
}

const ENERGY_LABELS: Record<string, string> = {
  GRASS: 'Planta',
  FIRE: 'Fogo',
  WATER: 'Água',
  LIGHTNING: 'Elétrico',
  PSYCHIC: 'Psíquico',
  FIGHTING: 'Lutador',
  DARKNESS: 'Escuridão',
  METAL: 'Metal',
  FAIRY: 'Fada',
  DRAGON: 'Dragão',
  COLORLESS: 'Incolor',
}

const EVOLUTION_LABELS: Record<string, string> = {
  BASIC: 'Básico',
  STAGE_1: 'Estágio 1',
  STAGE_2: 'Estágio 2',
}

const ATTRIBUTE_LABELS: Record<string, string> = {
  DARK: 'Trevas',
  LIGHT: 'Luz',
  DIVINE: 'Divino',
  FIRE: 'Fogo',
  WATER: 'Água',
  EARTH: 'Terra',
  WIND: 'Vento',
}

const INVOCATION_LABELS: Record<string, string> = {
  NORMAL: 'Normal',
  FUSION: 'Fusão',
  SYNCHRO: 'Sincro',
  XYZ: 'Xyz',
  LINK: 'Link',
  RITUAL: 'Ritual',
  PENDULUM: 'Pêndulo',
}

const COLOR_LABELS: Record<string, string> = {
  WHITE: 'Branco',
  BLACK: 'Preto',
  GREEN: 'Verde',
  BLUE: 'Azul',
  RED: 'Vermelho',
  COLORNESS: 'Incolor',
}

const SOURCE_LABELS: Record<string, string> = {
  POKEMON_TCG_API: 'Pokémon TCG API',
  YGOPRODECK: 'YGOPRODeck',
  SCRYFALL: 'Scryfall',
}

export type InfoRow = { label: string; value: string }

function label(map: Record<string, string>, value: string) {
  return map[value] ?? value
}

function present(
  value: string | number | null | undefined,
): value is string | number {
  return value !== null && value !== undefined && String(value).trim() !== ''
}

function rows(entries: Array<[string, string | number | null | undefined]>) {
  return entries
    .filter((entry): entry is [string, string | number] => present(entry[1]))
    .map(([rowLabel, value]) => ({ label: rowLabel, value: String(value) }))
}

function joined(values: string[], map?: Record<string, string>) {
  const items = values.filter(Boolean).map((v) => (map ? label(map, v) : v))
  return items.length ? items.join(', ') : null
}

/** `151 • Nº 025/165`, só com as partes que existirem. */
export function setLine(card: CatalogCardDetails) {
  return [card.setName, card.cardNumber ? `Nº ${card.cardNumber}` : null]
    .filter(present)
    .join(' • ')
}

export function imageAlt(card: CatalogCardDetails) {
  const line = [card.setName, card.cardNumber ? `Nº ${card.cardNumber}` : null]
    .filter(present)
    .join(', ')
  return line ? `${card.name} — ${line}` : card.name
}

/** Raridade primeiro, depois até dois atributos de destaque do TCG. */
export function highlightChips(card: CatalogCardDetails) {
  const chips: string[] = []
  if (card.pokemon) {
    chips.push(label(CARD_TYPE_LABELS, card.pokemon.cardType))
    if (card.pokemon.energyTypes[0])
      chips.push(label(ENERGY_LABELS, card.pokemon.energyTypes[0]))
  }
  if (card.yugioh) {
    chips.push(label(CARD_TYPE_LABELS, card.yugioh.cardType))
    if (card.yugioh.attribute)
      chips.push(label(ATTRIBUTE_LABELS, card.yugioh.attribute))
  }
  if (card.magic) {
    if (card.magic.colors[0])
      chips.push(label(COLOR_LABELS, card.magic.colors[0]))
    if (card.magic.cardTypes[0])
      chips.push(label(CARD_TYPE_LABELS, card.magic.cardTypes[0]))
  }
  return chips.filter(Boolean).slice(0, 2)
}

export function commonRows(card: CatalogCardDetails): InfoRow[] {
  return rows([
    ['Coleção', card.setName],
    ['Código da coleção', card.setCode],
    ['Número', card.cardNumber],
    ['Raridade', card.rarity],
  ])
}

export function gameRows(card: CatalogCardDetails): InfoRow[] {
  const { pokemon, yugioh, magic } = card
  if (pokemon) {
    return rows([
      ['Tipo', label(CARD_TYPE_LABELS, pokemon.cardType)],
      ['HP', pokemon.hp],
      [
        'Estágio',
        pokemon.evolutionStage
          ? label(EVOLUTION_LABELS, pokemon.evolutionStage)
          : null,
      ],
      ['Tipo de energia', joined(pokemon.energyTypes, ENERGY_LABELS)],
      ['Custo de recuo', pokemon.retreatCost],
    ])
  }
  if (yugioh) {
    const stats =
      yugioh.atk !== null && yugioh.def !== null
        ? `${yugioh.atk} / ${yugioh.def}`
        : yugioh.atk
    return rows([
      ['Tipo', label(CARD_TYPE_LABELS, yugioh.cardType)],
      [
        'Atributo',
        yugioh.attribute ? label(ATTRIBUTE_LABELS, yugioh.attribute) : null,
      ],
      ['Tipo de monstro', yugioh.monsterType],
      [yugioh.def !== null ? 'ATK / DEF' : 'ATK', stats],
      ['Nível', yugioh.level],
      ['Rank', yugioh.rank],
      ['Link', yugioh.linkRating],
      ['Invocação', joined(yugioh.invocationForms, INVOCATION_LABELS)],
    ])
  }
  if (magic) {
    const creature = present(magic.power) && present(magic.toughness)
    return rows([
      ['Tipo', joined(magic.cardTypes, CARD_TYPE_LABELS)],
      ['Supertipo', joined(magic.superTypes)],
      ['Subtipo', joined(magic.subTypes)],
      ['Cor', joined(magic.colors, COLOR_LABELS)],
      ['Custo de mana', magic.manaCost],
      ['Custo convertido', magic.cmc],
      [
        'Poder / Resistência',
        creature ? `${magic.power} / ${magic.toughness}` : null,
      ],
    ])
  }
  return []
}

export function rulesText(card: CatalogCardDetails) {
  return card.magic?.rulesText?.trim() || null
}

export function attacks(card: CatalogCardDetails) {
  return card.pokemon?.attacks ?? []
}

export function attackCost(attack: PokemonAttack) {
  return joined(attack.cost, ENERGY_LABELS)
}

export function sourceRows(card: CatalogCardDetails): InfoRow[] {
  return rows([
    ['Fonte dos dados', card.source ? label(SOURCE_LABELS, card.source) : null],
    ['Identificador na fonte', card.externalId],
  ])
}

/** Formato esperado pelo modal de adicionar à coleção. */
export function toCatalogCard(card: CatalogCardDetails): CatalogCard {
  return {
    id: card.id,
    name: card.name,
    game: GAME_INFO[card.cardGame].name,
    edition: card.setName ?? '',
    cardNumber: card.cardNumber ?? '',
    officialRarity: card.rarity || 'Não informada',
    imageUrl: card.imageUrl || '',
  }
}
