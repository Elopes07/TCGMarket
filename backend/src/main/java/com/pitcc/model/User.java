package com.pitcc.model;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class User {

  @Id private String id;

  private String name;

  @Indexed(unique = true)
  private String email;

  private String passwordHash;

  private UserRole role;

  private Instant registrationDate;

  // Esparso porque contas anteriores ao perfil só recebem username na subida da aplicação.
  @Indexed(unique = true, sparse = true)
  private String username;

  private String bio;

  private String city;

  private String state;

  private List<CardGame> favoriteGames;

  private List<String> links;

  public User() {}

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public UserRole getRole() {
    return role;
  }

  public void setRole(UserRole role) {
    this.role = role;
  }

  public Instant getRegistrationDate() {
    return registrationDate;
  }

  public void setRegistrationDate(Instant registrationDate) {
    this.registrationDate = registrationDate;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getBio() {
    return bio;
  }

  public void setBio(String bio) {
    this.bio = bio;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public List<CardGame> getFavoriteGames() {
    return favoriteGames;
  }

  public void setFavoriteGames(List<CardGame> favoriteGames) {
    this.favoriteGames = favoriteGames;
  }

  public List<String> getLinks() {
    return links;
  }

  public void setLinks(List<String> links) {
    this.links = links;
  }
}
