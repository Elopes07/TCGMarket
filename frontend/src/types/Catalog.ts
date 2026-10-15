export interface CatalogCard {
  id: string
  name: string
  game: string
  edition: string
  cardNumber: string
  officialRarity: string
  imageUrl: string
}

export type CatalogSort = 'name-asc' | 'name-desc'

/** Estado da busca na URL do catálogo. {@link page} começa em 1. */
export interface CatalogSearch {
  q?: string
  set?: string
  rarity?: string
  type?: string
  sort?: CatalogSort
  page?: number
}

export interface CatalogPage {
  content: CatalogCard[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CatalogPageResponse {
  content: CatalogCardResponse[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CatalogFilters {
  sets: string[]
  rarities: string[]
  types: string[]
}

export type CardGame = 'POKEMON' | 'YUGIOH' | 'MAGIC_THE_GATHERING'

export interface CatalogCardResponse {
  id: string
  externalId: string
  conceptualId: string
  name: string
  cardGame: CardGame
  setName: string | null
  setCode: string | null
  cardNumber: string | null
  rarity: string | null
  imageUrl: string | null
}

export interface PokemonAttack {
  name: string
  cost: string[]
  damage: string | null
}

export interface PokemonDetails {
  cardType: string
  hp: number | null
  energyTypes: string[]
  evolutionStage: string | null
  retreatCost: number | null
  attacks: PokemonAttack[]
}

export interface YugiohDetails {
  cardType: string
  attribute: string | null
  monsterType: string | null
  atk: number | null
  def: number | null
  level: number | null
  rank: number | null
  linkRating: number | null
  invocationForms: string[]
}

export interface MagicDetails {
  cardTypes: string[]
  colors: string[]
  manaCost: string | null
  cmc: number | null
  power: string | null
  toughness: string | null
  superTypes: string[]
  subTypes: string[]
  rulesText: string | null
}

export interface CardPrinting {
  id: string
  setName: string | null
  cardNumber: string | null
  rarity: string | null
}

/** Carta completa do catálogo interno, buscada pelo id em `/cartas/:id`. */
export interface CatalogCardDetails {
  id: string
  externalId: string
  conceptualId: string | null
  name: string
  cardGame: CardGame
  setName: string | null
  setCode: string | null
  cardNumber: string | null
  rarity: string | null
  imageUrl: string | null
  source: 'POKEMON_TCG_API' | 'YGOPRODECK' | 'SCRYFALL' | null
  pokemon: PokemonDetails | null
  yugioh: YugiohDetails | null
  magic: MagicDetails | null
  otherPrintings: CardPrinting[]
}
