package com.pitcc.dto;

import com.pitcc.model.Card;
import com.pitcc.model.CardGame;
import com.pitcc.model.CardSource;
import com.pitcc.model.MagicCard;
import com.pitcc.model.MagicCardType;
import com.pitcc.model.MagicColor;
import com.pitcc.model.PokemonAttack;
import com.pitcc.model.PokemonCard;
import com.pitcc.model.PokemonCardType;
import com.pitcc.model.PokemonEnergyType;
import com.pitcc.model.PokemonEvolutionStage;
import com.pitcc.model.YugiohAttribute;
import com.pitcc.model.YugiohCard;
import com.pitcc.model.YugiohCardType;
import com.pitcc.model.YugiohInvocation;
import java.util.List;

/**
 * Carta completa do catálogo interno. Só um entre {@code pokemon}, {@code yugioh} e
 * {@code magic} vem preenchido, conforme o jogo da carta.
 */
public record CatalogCardDetailsResponse(
    String id,
    String externalId,
    String conceptualId,
    String name,
    CardGame cardGame,
    String setName,
    String setCode,
    String cardNumber,
    String rarity,
    String imageUrl,
    CardSource source,
    PokemonDetails pokemon,
    YugiohDetails yugioh,
    MagicDetails magic,
    List<Printing> otherPrintings) {

  public record PokemonDetails(
      PokemonCardType cardType,
      Integer hp,
      List<PokemonEnergyType> energyTypes,
      PokemonEvolutionStage evolutionStage,
      Integer retreatCost,
      List<Attack> attacks) {}

  public record Attack(String name, List<PokemonEnergyType> cost, String damage) {}

  public record YugiohDetails(
      YugiohCardType cardType,
      YugiohAttribute attribute,
      String monsterType,
      Integer atk,
      Integer def,
      Integer level,
      Integer rank,
      Integer linkRating,
      List<YugiohInvocation> invocationForms) {}

  public record MagicDetails(
      List<MagicCardType> cardTypes,
      List<MagicColor> colors,
      String manaCost,
      Double cmc,
      String power,
      String toughness,
      List<String> superTypes,
      List<String> subTypes,
      String rulesText) {}

  public record Printing(String id, String setName, String cardNumber, String rarity) {}

  public static CatalogCardDetailsResponse from(Card card, List<Card> otherPrintings) {
    PokemonDetails pokemon = card instanceof PokemonCard value ? pokemon(value) : null;
    YugiohDetails yugioh = card instanceof YugiohCard value ? yugioh(value) : null;
    MagicDetails magic = card instanceof MagicCard value ? magic(value) : null;
    List<Printing> printings = otherPrintings.stream()
        .map(other -> new Printing(
            other.getId(), other.getEdition(), other.getCardNumber(), other.getOfficialRarity()))
        .toList();

    return new CatalogCardDetailsResponse(
        card.getId(),
        card.getExternalId(),
        card.getConceptualId(),
        card.getName(),
        card.getGame(),
        card.getEdition(),
        card.getCodeCollection(),
        card.getCardNumber(),
        card.getOfficialRarity(),
        card.getImageUrl(),
        card.getSource(),
        pokemon,
        yugioh,
        magic,
        printings);
  }

  private static PokemonDetails pokemon(PokemonCard card) {
    List<Attack> attacks = listOf(card.getAttacks()).stream()
        .map(CatalogCardDetailsResponse::attack)
        .toList();
    return new PokemonDetails(
        card.getPokemonCardType(),
        card.getHp(),
        listOf(card.getEnergyTypes()),
        card.getEvolutionStage(),
        card.getRecoilCost(),
        attacks);
  }

  private static YugiohDetails yugioh(YugiohCard card) {
    return new YugiohDetails(
        card.getYugiohCardType(),
        card.getAttribute(),
        card.getMonsterType(),
        card.getAtk(),
        card.getDef(),
        card.getLevel(),
        card.getRank(),
        card.getLinkRating(),
        listOf(card.getInvocationForms()));
  }

  private static MagicDetails magic(MagicCard card) {
    return new MagicDetails(
        listOf(card.getCardTypes()),
        listOf(card.getColor()),
        card.getManaCost(),
        card.getCmc(),
        card.getStrength(),
        card.getResistance(),
        listOf(card.getSuperTypes()),
        listOf(card.getSubTypes()),
        card.getRulesText());
  }

  private static Attack attack(PokemonAttack attack) {
    return new Attack(attack.getName(), listOf(attack.getEnergyCost()), attack.getDamage());
  }

  // Documentos antigos no Mongo podem não ter as listas gravadas.
  private static <T> List<T> listOf(List<T> values) {
    return values == null ? List.of() : values;
  }
}
