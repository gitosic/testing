package project.pages;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Selenide.$x;

public class Elements {

    public static SelenideElement loginButton(String text) {
        return $x("//header//div[@class='s-topbar--container']//li/a[text()='" + text + "']");
    }
}
