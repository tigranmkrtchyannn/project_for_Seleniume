package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/login", (exchange -> {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                String params = exchange.getRequestURI().getQuery();
                String cookies = executeSelenium(params);
                sendResponse(exchange, 200, cookies);
            } else {

                sendResponse(exchange, 405, "");
            }
        }));
        server.setExecutor(null);
        server.start();
    }


    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static String executeSelenium(String requestBody) {
        String[] params = requestBody.split("&");
        String userLogin = "", userPassword = "", proxyName = "", proxyHost = "", proxyPassword = "", proxyType = "", userAgent = "", cookie = "";
        int proxyPort = 0;

        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                value = URLDecoder.decode(value, StandardCharsets.UTF_8);
                switch (key) {
                    case "userLogin":
                        userLogin = value;
                        break;
                    case "userPassword":
                        userPassword = value;
                        break;
                    case "proxyHost":
                        proxyHost = value;
                        break;
                    case "proxyName":
                        proxyName = value;
                        break;
                    case "proxyPassword":
                        proxyPassword = value;
                        break;
                    case "proxyPort":
                        proxyPort = Integer.parseInt(value);
                        break;
                    case "proxyType":
                        proxyType = value.toUpperCase();
                        break;
                    case "userAgent":
                        userAgent = value;
                        break;
                    case "cookie":
                        cookie = value;
                    default:
                        break;
                }
            }
        }


        Proxy proxy = createProxy(proxyHost, proxyPort, proxyType, proxyName, proxyPassword);
        WebDriver driver = initializeWebDriver(proxy, userAgent);
        addCookies(driver,cookie);

        driver.get("https://www.facebook.com/");
        loginFacebook(driver, userLogin, userPassword);

        Set<Cookie> cookies = driver.manage().getCookies();
        StringBuilder cookieInfo = new StringBuilder();
        for (Cookie seleniumCookie : cookies) {
            cookieInfo.append(seleniumCookie.getName()).append("=").append(seleniumCookie.getValue()).append("; ");
        }
        return cookieInfo.toString();
    }

    private static void loginFacebook(WebDriver driver, String email, String password) {
        WebElement emailInput = driver.findElement(By.id("email"));
        emailInput.sendKeys(email);

        WebElement passwordInput = driver.findElement(By.id("pass"));
        passwordInput.sendKeys(password);

        WebElement loginButton = driver.findElement(By.name("login"));
        loginButton.click();
    }

    public static Proxy createProxy(String proxyHost, int proxyPort, String proxyType, String proxyName, String proxyPassword) {
        String proxyString = proxyHost + ":" + proxyPort;
        Proxy proxy = new Proxy();

        proxy.setProxyType(Proxy.ProxyType.MANUAL).setHttpProxy(null).setSocksProxy(null);

        if (proxyType.equalsIgnoreCase("HTTP")) {
            proxy.setHttpProxy(proxyString);
        } else if (proxyType.equalsIgnoreCase("SOCKS5")) {
            proxy.setSocksProxy(proxyString).setSocksVersion(5);
        } else if (proxyType.equalsIgnoreCase("SOCKS4")) {
            proxy.setSocksProxy(proxyString).setSocksVersion(4);
        }

        if (proxyName != null && !proxyName.isEmpty() && proxyPassword != null && !proxyPassword.isEmpty()) {
            proxy.setSocksUsername(proxyName).setSocksPassword(proxyPassword);
        }

        return proxy;
    }

    public static WebDriver initializeWebDriver(Proxy proxy, String userAgent) {
        ChromeOptions options = new ChromeOptions();
        if (proxy != null) {
            options.setProxy(proxy);
        }
        options.addArguments("--user-agent=" + userAgent);

        return new ChromeDriver(options);
    }

    private static void addCookies(WebDriver driver, String cookiesString) {
        String[] cookiesArray = cookiesString.split("; ");
        for (String cookieString : cookiesArray) {
            String[] cookieParts = cookieString.split("=");
            if (cookieParts.length == 2) {
                String name = cookieParts[0];
                String value = cookieParts[1];
                Cookie cookie = new Cookie(name, value);
                driver.manage().addCookie(cookie);
            }
        }
    }

}
