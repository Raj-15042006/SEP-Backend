package com.passport.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {
    private String url = "https://lwyknurgoianubqadfsr.supabase.co";
    private String anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imx3eWtudXJnb2lhbnVicWFkZnNyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODMzMDExOTIsImV4cCI6MjA5ODg3NzE5Mn0.9_kpnyFNXQ_gtVz4ppjIERvbupG5eLefT2C7DyPi7iI";
}
