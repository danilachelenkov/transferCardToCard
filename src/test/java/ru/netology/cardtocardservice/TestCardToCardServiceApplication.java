package ru.netology.cardtocardservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestCardToCardServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(CardToCardServiceApplication::main).with(TestCardToCardServiceApplication.class).run(args);
	}

}
