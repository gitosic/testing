@UiTests
Feature: Фича файл с кейсами

  @TestQA1
  Scenario: Простой UI тест
    When открыть страницу
    Then в хедоре присутствует кнопка "Log in"

  @TestQA2
  Scenario: Тест выполнения
    When вывести тест "Привет Мир!"
    Then текст появился