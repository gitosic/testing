package project.utils;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.SharedDownloadsFolder;
import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.openqa.selenium.Platform;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverLogLevel;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.CapabilityType;
import project.config.TestConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class HelperClass {

    // "chrome" or "chrome-headless"
    private static final String browserType = System.getProperty("browser.type", "chrome-headless");
    private static final String sslProxy = "http://blablabla@abc.com:PassWord@isa-dev-proxy.intranet.aaa.com:8080";
    private static final String httpProxy = "http://blablabla@abc.com:PassWord@isa-dev-proxy.intranet.aaa.com:8080";
    private static final String noProxy = System.getProperty("browser.noproxy", System.getenv("no_proxy"));
    private static final String separator = System.getProperty("file.separator");
    private static final String defaultLocalPathNameUiDownloads =
            System.getProperty("user.home") + separator + "Downloads" + separator + "UI_Downloads" + separator;
    private static final ChromeDriverService BROWSER_SERVICE;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final boolean LOCKING = true;

    // You can customize the configuration com.codeborne.selenide.SelenideConfig
    static {
        Configuration.reopenBrowserOnFail = false;
        Configuration.holdBrowserOpen = false;
        Configuration.timeout = 30000;
        Configuration.reportsFolder = "target/reports/tests";
        try {
            BROWSER_SERVICE = createBrowserService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getDriverPath() {
        Path driverPath;
        if (Platform.getCurrent().is(Platform.WINDOWS)) {
            // Adding "chromedriver.exe" for Windows
            driverPath = Path.of("src\\test\\resources\\chromedriver\\chromedriver.exe");
            if (!driverPath.toFile().exists()) {
                driverPath = Path.of("project-ui-e2e-tests\\src\\test\\resources\\chromedriver\\chromedriver.exe");
            }
        } else {
            // https://chromedriver.storage.googleapis.com/index.html
            // Adding "chromedriver" for Linux
            driverPath = Path.of("src/test/resources/chromedriver/chromedriver");
            if (!driverPath.toFile().exists()) {
                driverPath = Path.of("src/test/resources/chromedriver/chromedriver");
            }
        }
        return driverPath;
    }

    public static ChromeDriverService createBrowserService() throws IOException {
        ChromeDriverService service = new ChromeDriverService.Builder()
                .withLogLevel(ChromeDriverLogLevel.WARNING)
                .usingDriverExecutable(getDriverPath().toFile())
                .usingAnyFreePort()
                .withTimeout(Duration.ofSeconds(30))
                .build();
        service.start();
        return service;
    }

    public static ChromeOptions getChromeOptions() {
        var options = new ChromeOptions();
        options.addArguments("disable-infobars", "--disable-extensions", "--disable-gpu", "--disable-dev-shm-usage",
                "--no-zygote", "--no-sandbox", "--ignore-certificate-errors", "--ignore-ssl-errors=yes");
        if ("chrome-headless".equals(browserType)) {
            options.addArguments("--headless");
            options.addArguments("--window-size=2560, 1440");
            options.addArguments("--remote-allow-origins=*");
        } else {
            options.addArguments("--start-maximized");
            options.addArguments("--remote-allow-origins=*");
        }
        // options.setExperimentalOption("w3c", true);
        var logPrefs = new LoggingPreferences();
        logPrefs.enable("browser", Level.INFO);
        logPrefs.enable("performance", Level.INFO);
        options.setCapability("goog:loggingPrefs", logPrefs);
        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
        return options;
    }

    private static ChromeDriverService getBrowserService() {
        return BROWSER_SERVICE; //ChromeDriverService.createDefaultService();
    }

    private static WebDriver getWebDriver(String testUuid) {
        try {
            System.err.println("Trying to acquire readLock " + Thread.currentThread());
            if (LOCKING && !lock.readLock().tryLock() && !lock.readLock().tryLock(60, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to readLock for getWebDriver " + Thread.currentThread());
            }
            System.err.println("Success acquire readLock " + Thread.currentThread());

            if (WebDriverRunner.hasWebDriverStarted()) {
                return WebDriverRunner.getWebDriver();
            }

        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to readLock for getWebDriver " + Thread.currentThread());
        } finally {
            if (LOCKING) {
                lock.readLock().unlock();
            }
        }

        ChromeOptions options = getChromeOptions();

        var seleniumProxy = new Proxy();
        seleniumProxy.setSslProxy(StringUtils.isEmpty(sslProxy) ? null : sslProxy);
        seleniumProxy.setHttpProxy(StringUtils.isEmpty(httpProxy) ? null : httpProxy);
        seleniumProxy.setNoProxy(StringUtils.isEmpty(noProxy) ? null : noProxy);

        if (!StringUtils.isEmpty(sslProxy) || !StringUtils.isEmpty(httpProxy)) {
            options.setCapability(CapabilityType.PROXY, seleniumProxy);
        }

        System.err.println("Start new ChromeDriver " + Thread.currentThread());
        var driver = new ChromeDriver(getBrowserService(), options);
        System.err.println("Done new ChromeDriver " + Thread.currentThread());

        String downloadPath =
                defaultLocalPathNameUiDownloads + ((testUuid != null) ? testUuid + separator : "");
        var commandParams = new HashMap<String, Object>();
        commandParams.put("cmd", "Page.setDownloadBehavior");
        var params = new HashMap<String, String>();
        params.put("behavior", "allow");
        params.put("downloadPath", downloadPath);
        commandParams.put("params", params);
        var objectMapper = new ObjectMapper();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            var command = objectMapper.writeValueAsString(commandParams);
            var u = BROWSER_SERVICE.getUrl().toString() + "/session/" + driver.getSessionId() +
                    "/chromium/send_command";
            var request = new HttpPost(u);
            request.addHeader("content-type", "application/json");
            request.setEntity(new StringEntity(command));
            httpClient.execute(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            System.err.println("Trying to acquire writeLock " + Thread.currentThread());
            if (LOCKING && !lock.writeLock().tryLock() && !lock.writeLock().tryLock(60, TimeUnit.SECONDS)) {
                throw new RuntimeException("Failed to writeLock for getWebDriver");
            }
            System.err.println("Success acquire writeLock " + Thread.currentThread());
            System.err.println("WebDriverRunner.setWebDriver " + Thread.currentThread());
            WebDriver protect = driver; //ThreadGuard.protect(driver);
            WebDriverRunner.setWebDriver(protect, null, new SharedDownloadsFolder(downloadPath));
            System.err.println("Unlocked writeLock " + Thread.currentThread());
            return protect;
        } catch (Throwable e) {
            throw new RuntimeException("Failed to getWebDriver " + Thread.currentThread(), e);
        } finally {
            if (LOCKING && lock.writeLock().isHeldByCurrentThread()) {
                lock.writeLock().unlock();
            }
        }
    }

    public static void openUrl(String uri) {
        WebDriver webDriver;
        webDriver = getWebDriver("");
        webDriver.get(uri);
    }

    public static void openUrlPage() {
        String url = TestConfig.getConfig().uiUrl();
        WebDriver webDriver;
        webDriver = getWebDriver("");
        webDriver.get(url);
    }

    static synchronized void closeDrivers() {
        System.err.println("ChromiumHelpers closeDriver");
        WebDriverRunner.getWebDriver().close();
        WebDriverRunner.getWebDriver().quit();
        WebDriverRunner.webdriverContainer.closeWindow();
        WebDriverRunner.webdriverContainer.closeWebDriver();
        BROWSER_SERVICE.close();
    }

    public static void closeApp() {
        System.err.println("ChromiumHelpers closeApp");
        closeDrivers();
    }
}