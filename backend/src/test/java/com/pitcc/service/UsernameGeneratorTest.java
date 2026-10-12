package com.pitcc.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.pitcc.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsernameGeneratorTest {

  @Mock private UserRepository userRepository;

  @Test
  void shouldJoinFirstAndLastNameWithoutAccents() {
    assertEquals("joao.silva", UsernameGenerator.base("João da Silva"));
    assertEquals("ana.kimura", UsernameGenerator.base("  Ana Paula KIMURA "));
  }

  @Test
  void shouldKeepSingleNames() {
    assertEquals("aline", UsernameGenerator.base("Aline"));
  }

  @Test
  void shouldFallBackWhenNameHasNoUsableCharacters() {
    assertEquals("jogador", UsernameGenerator.base("!!!"));
    assertEquals("jogador", UsernameGenerator.base("Zé"));
    assertEquals("jogador", UsernameGenerator.base(null));
  }

  @Test
  void shouldLimitLengthWithoutTrailingDot() {
    String base = UsernameGenerator.base("Maximiliano Bartholomew");

    assertTrue(base.length() <= 20);
    assertEquals("maximiliano.barthol", base.substring(0, 19));
    assertTrue(!base.endsWith("."));
  }

  @Test
  void shouldAppendSuffixWhenUsernameIsTaken() {
    UsernameGenerator generator = new UsernameGenerator(userRepository);
    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByUsername("joao.silva")).thenReturn(true);
    when(userRepository.existsByUsername("joao.silva2")).thenReturn(true);

    assertEquals("joao.silva3", generator.generate("João Silva"));
  }

  @Test
  void shouldShortenBaseToFitSuffix() {
    UsernameGenerator generator = new UsernameGenerator(userRepository);
    when(userRepository.existsByUsername(anyString())).thenReturn(false);
    when(userRepository.existsByUsername("maximiliano.bartholo")).thenReturn(true);

    String generated = generator.generate("Maximiliano Bartholomew");

    assertEquals("maximiliano.barthol2", generated);
    assertEquals(20, generated.length());
  }
}
