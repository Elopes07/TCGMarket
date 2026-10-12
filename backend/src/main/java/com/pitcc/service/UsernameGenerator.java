package com.pitcc.service;

import com.pitcc.repository.UserRepository;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class UsernameGenerator {

  static final int MAX_LENGTH = 20;
  static final int MIN_LENGTH = 3;
  static final String FALLBACK = "jogador";

  private final UserRepository userRepository;

  public UsernameGenerator(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /** Primeiro e último nome, sem acentos, unidos por ponto; sufixo numérico se já existir. */
  public String generate(String name) {
    String base = base(name);
    if (!userRepository.existsByUsername(base)) {
      return base;
    }
    for (int suffix = 2; ; suffix++) {
      String tail = Integer.toString(suffix);
      String candidate = trimDots(base.substring(0, Math.min(base.length(), MAX_LENGTH - tail.length()))) + tail;
      if (!userRepository.existsByUsername(candidate)) {
        return candidate;
      }
    }
  }

  static String base(String name) {
    String plain =
        Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
    List<String> words =
        Arrays.stream(plain.split("[^a-z0-9]+")).filter(word -> !word.isEmpty()).toList();
    if (words.isEmpty()) {
      return FALLBACK;
    }
    String joined =
        words.size() == 1 ? words.getFirst() : words.getFirst() + "." + words.getLast();
    String trimmed = trimDots(joined.substring(0, Math.min(joined.length(), MAX_LENGTH)));
    return trimmed.length() < MIN_LENGTH ? FALLBACK : trimmed;
  }

  private static String trimDots(String value) {
    return value.replaceAll("^\\.+|\\.+$", "");
  }
}
