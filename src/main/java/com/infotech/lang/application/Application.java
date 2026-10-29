package com.infotech.lang.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

/**
 * The entry point of the Spring Boot application.
 */
@SpringBootApplication
@EnableAsync
@NpmPackage(value = "lumo-css-framework", version = "^4.0.10")
@Theme("flowcrmtutorial")
@PWA(
    name = "LangSkills",
    shortName = "LangSkills",
    backgroundColor = "#ffffff",
    themeColor = "#6200ea",
    iconPath = "icons/icon.png",
    offlinePath = "offline.html",
    offlineResources = { "./images/offline.png" }
)
@JavaScript("context://install-prompt.js")
public class Application implements AppShellConfigurator {
private static final long serialVersionUID = 1L;

//public class Application extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
