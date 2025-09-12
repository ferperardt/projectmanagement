# 🚀 Project and Task Management System

# **Spring Boot + Spring Security Portfolio Project**

---

## 🎯 Objective

Develop a complete project management system demonstrating advanced Spring Security concepts, from basic authentication
to 2FA, incrementally and practically.

graph TD
A[Request com JWT] --> B[JwtAuthenticationFilter]
B --> C{Token válido?}
C -->|Sim| D[Cria Authentication object]
D --> E[SecurityContext.setAuthentication]
E --> F[Spring Security @PreAuthorize/@Secured]
F --> G[Controller endpoint]
C -->|Não| H[Request sem autenticação]
H --> I[Spring Security bloqueia se protegido]