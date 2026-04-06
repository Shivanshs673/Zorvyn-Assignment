package com.example.zorvynassignment

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.CommandLineRunner
import java.time.Clock

@Configuration
class AppConfig {
    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()

    @Bean
    fun seedUsersOnStartup(dataSeeder: DataSeeder): CommandLineRunner = CommandLineRunner {
        dataSeeder.seedDefaultUsers()
    }
}