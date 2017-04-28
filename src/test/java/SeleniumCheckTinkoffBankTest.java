import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jvnet.jaxb2_commons.lang.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;


@RunWith(Parameterized.class)
public class SeleniumCheckTinkoffBankTest {
    private final String payerCode;
    private final String payPeriod;
    private final String paymentAmount;

    private static final String ERR_MSG_FIELD_REQ = "Поле обязательное";
    private static final String ERR_MSG_FIELD_INCRORRECT = "Поле неправильно заполнено";
    private static final String ERR_MSG_FIELD_MIN_AMOUNT = "Минимальная сумма перевода - 10 \u20BD";
    private static final String ERR_MSG_FIELD_MAX_AMOUNT = "Максимальная сумма перевода - 15 000 \u20BD";
    private static final String ERR_MSG_PERIOD_INCRORRECT = "Поле заполнено некорректно";

    private WebDriver driver;
    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        final List<Object[]> cases = new LinkedList<>();

        Path filePath = Paths.get(new File("").getAbsolutePath(), "testdata", "data.txt");

        try (final Scanner reader = new Scanner(new File(filePath.toString()))) {
            while (reader.hasNext()) {
                String[] raw = reader.nextLine().trim().split(";");
                String payerCode = raw[0];
                String payPeriod = raw[1];
                String paymentAmount = raw[2];
                cases.add(new Object[] { payerCode, payPeriod, paymentAmount});
            }
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't load data", e);
        }
        return cases;
    }

    public SeleniumCheckTinkoffBankTest(String payerCode, String payPeriod, String paymentAmount) {
        this.payerCode = payerCode;
        this.payPeriod = payPeriod;
        this.paymentAmount = paymentAmount;
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-notifications");
        this.driver = new ChromeDriver(options);
        System.out.println("[Step info] Open start page.");
        driver.get("https://www.tinkoff.ru");
    }

    @Test
    public void checkPaymentAttributesForFirstMoscowServiceProvider() {
        this._navigateToCommunalPaymentPage();
        this._getRegionLink().click();
        this._openFirstMoscowServiceProvider();
        this._navigateToPaymentTab();
        System.out.println("[Step info] Insert incorrect payer code: " + payerCode);
        this._getElementByXpath("//input[@id='payerCode']").sendKeys(payerCode);

        System.out.println("[Step info] Insert incorrect payment period: " + payPeriod);
        this._getElementByXpath("//span[text()='За какой период оплачиваете коммунальные услуги']/../input").sendKeys(payPeriod);

        System.out.println("[Step info] Insert incorrect payment amount: " + paymentAmount);
        this._getElementByXpath("//b[text()='Сумма платежа, ']/../input").sendKeys(paymentAmount);

        this._getElementByXpath("//h2[text()='Оплатить ЖКУ в Москве']").click();
        this._validateErrorMessageForPayerCode();
        this._validateErrorMessageForPayPeriod();
        this._validateErrorMessageForPaymentAmount();
        driver.quit();
    }

    @Test
    public void checkRegionalServiceProviderUi() throws InterruptedException {
        this._navigateToCommunalPaymentPage();

        System.out.println("[Step info] Verify autodetected region.");
        String regionName = this._getRegionLink().getText();
        if (!regionName.equals("Москве")) {
            System.out.println("Incorrect region was selected. Expected: Москве. Actual: " + regionName);
            this._getRegionLink().click();
        }

        String expectedServiceProviderName = this._openFirstMoscowServiceProvider();

        System.out.println("[Step info] Open payment tab.");
        this._navigateToPaymentTab();
        TimeUnit.SECONDS.sleep(15);//workaround for extremely long loading
        String paymentPageTitle = driver.getTitle();

        this._navigateToCommunalPaymentPage();

        System.out.println("[Step info] Find service provider " + expectedServiceProviderName + " via quick search'.");
        this._getElementByXpath("//input[@class='ui-search-input__input']").sendKeys(expectedServiceProviderName);

        System.out.println("[Step info] Make sure that searched service provider will be the first in list.");
        WebElement firstFindServiceProviderLink = this._getElementByXpath("//div[@class='ui-search-flat']/span[1]//div[@class='ui-search-flat__title-box']");
        String actualServiceProviderName = firstFindServiceProviderLink.getText();
        Assert.assertTrue(
                "Service provider name was not find. Expected: " + expectedServiceProviderName + ".Actual: " + actualServiceProviderName,
                actualServiceProviderName.equals(expectedServiceProviderName)
        );

        System.out.println("[Step info] Open payment tab for selected service provider.");
        firstFindServiceProviderLink.click();
        this._navigateToPaymentTab();

        System.out.println("[Step info] Make sure that page is the same as after direct access.");
        String paymentPageTitleAfterSearch = driver.getTitle();
       Assert.assertTrue(
                "Webpage does not equal with required. Expected: " + paymentPageTitle + ".Actual: " + paymentPageTitleAfterSearch,
                paymentPageTitle.equals(paymentPageTitleAfterSearch)
        );

        System.out.println("[Step info] Select St.Petersburg region.");
        this._navigateToCommunalPaymentPage();
        this._getRegionLink().click();
        this._getElementByXpath("//span[text()='г. Санкт-Петербург']").click();

        System.out.println("[Step info] Check that Moscow service provider is absent in list of available service providers in St.Petersburg region.");
        List<WebElement> elements = driver.findElements(By.xpath("//ul[@class='ui-menu ui-menu_icons']/li"));
        for (WebElement element: elements) {
            String serviceProviderName = element.getText();
            Assert.assertNotEquals(serviceProviderName,expectedServiceProviderName);
        }
        driver.quit();
    }

    private String _openFirstMoscowServiceProvider() {
        System.out.println("[Step info] Select Moscow region.");
        this._getElementByXpath("//span[text()='г. Москва']").click();

        System.out.println("[Step info] Select first service provider from list.");
        WebElement serviceProviderLink = this._getElementByXpath("//ul[@class='ui-menu ui-menu_icons']/li[1]");
        serviceProviderLink.click();
        return serviceProviderLink.getText();
    }

    private WebElement _getRegionLink() {
        return this._getElementByXpath("//div[text()='Коммунальные платежи']/span[2]");
    }

    private void _navigateToCommunalPaymentPage() {
        System.out.println("[Step info] Navigate to tab 'Платежи'.");
        this._getElementByXpath("//span[text()='Платежи']").click();

        System.out.println("[Step info] Navigate to tab 'Коммунальные платежи'.");
        this._getElementByXpath("//span[text()='Коммунальные платежи']").click();
  }

    private WebElement _getElementByXpath(String xpath) {
        return (new WebDriverWait(driver,20).until(ExpectedConditions.elementToBeClickable(By.xpath(xpath))));
    }

    private void _navigateToPaymentTab() {
        this._getElementByXpath("//span[text()='Оплатить ЖКУ в Москве']").click();
    }

    private void _validateErrorMessageForPayerCode() {
        String ErrorMsg = this._getElementByXpath("//input[@id='payerCode']/ancestor::div[contains(@class,'ui-form__field')]//div[contains(@class,'ui-form-field-error-message_ui-form')]").getText();
        if (StringUtils.isEmpty(payerCode)) {
            Assert.assertEquals(
                    "Incorrect error message for payer code. Expected: " + ERR_MSG_FIELD_REQ + ". Actual: " + ErrorMsg,
                    ErrorMsg,ERR_MSG_FIELD_REQ
            );
        } else {
            Assert.assertEquals(
                    "Incorrect error message for payer code. Expected: " + ERR_MSG_FIELD_INCRORRECT + ". Actual: " + ErrorMsg,
                    ErrorMsg, ERR_MSG_FIELD_INCRORRECT
            );
        }
    }

    private void _validateErrorMessageForPayPeriod() {
        String ErrorMsg = this._getElementByXpath("//input[@name='provider-period']/ancestor::div[contains(@class,'ui-form__field')]//div[contains(@class,'ui-form-field-error-message_ui-form')]").getText();
        if(_isInteger(payPeriod)) {
            Assert.assertEquals(
                    "Incorrect error message for payment period. Expected: " + ERR_MSG_PERIOD_INCRORRECT + ". Actual: " + ErrorMsg,
                    ErrorMsg,ERR_MSG_PERIOD_INCRORRECT
            );
        } else if (StringUtils.isEmpty(payPeriod)) {
            Assert.assertEquals(
                    "Incorrect error message for payment period. Expected: " + ERR_MSG_FIELD_REQ + ". Actual: " + ErrorMsg,
                    ErrorMsg,ERR_MSG_FIELD_REQ
            );
        }

    }

    private void _validateErrorMessageForPaymentAmount() {
        String ErrorMsg = this._getElementByXpath("//b[text()='Сумма платежа, ']/../input//ancestor::div[contains(@class,'ui-form__field')]//div[contains(@class,'ui-form-field-error-message_ui-form')]").getText();
        if(_isInteger(paymentAmount)) {
            if (Integer.parseInt(paymentAmount)<10) {
                Assert.assertEquals(
                        "Incorrect error message for payment amount. Expected: " + ERR_MSG_FIELD_MIN_AMOUNT + ". Actual: " + ErrorMsg,
                        ErrorMsg,ERR_MSG_FIELD_MIN_AMOUNT
                );
            } else if (Integer.parseInt(paymentAmount)>15001) {
                Assert.assertEquals(
                        "Incorrect error message for payment amount. Expected: " + ERR_MSG_FIELD_MAX_AMOUNT + ". Actual: " + ErrorMsg,
                        ErrorMsg,ERR_MSG_FIELD_MAX_AMOUNT
                );
            }
        } else {
            Assert.assertEquals(
                    "Incorrect error message. Expected: " + ERR_MSG_FIELD_REQ + ". Actual: " + ErrorMsg,
                    ErrorMsg,ERR_MSG_FIELD_REQ
            );
        }
    }

    private static boolean _isInteger(String str) {
        try {
            Integer.parseInt(str);
        } catch(NumberFormatException e) {
            return false;
        }
        return true;
    }
}
