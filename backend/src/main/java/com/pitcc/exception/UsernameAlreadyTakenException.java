package com.pitcc.exception;

public class UsernameAlreadyTakenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public static final String MESSAGE = "Este nome de usuário já está em uso.";

  public UsernameAlreadyTakenException() {
    super(MESSAGE);
  }
}
