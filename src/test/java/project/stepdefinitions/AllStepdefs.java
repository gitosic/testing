package project.stepdefinitions;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static com.codeborne.selenide.Condition.visible;
import static project.pages.Elements.loginButton;
import static project.utils.HelperClass.openUrlPage;

public class AllStepdefs {

    @When("вывести тест {string}")
    public void вывестиТест(String text) {
        System.out.println(text);
    }

    @Then("текст появился")
    public void текстПоявился() {
        System.out.println("Текст появился");
    }

    @When("открыть страницу")
    public void открытьСтраницу() {
        openUrlPage();
    }

    @Then("в хедоре присутствует кнопка {string}")
    public void вХедореПрисутствует(String text) {
        loginButton(text).shouldBe(visible);

    }
}
