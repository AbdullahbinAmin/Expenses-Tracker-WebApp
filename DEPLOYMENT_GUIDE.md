# Spring Boot + MySQL Docker Deployment Guide (Bilingual: Roman Urdu & English)

Aapke request ke mutabik, saari purani Dockerfiles, docker-compose.yml aur build configurations ko delete kar diya gaya hai. Ab aapka workspace ekdum clean hai, jaise developer ne fresh code diya ho.

Aap is guide ko follow karke step-by-step khud practice kar sakte hain aur production-level deployment seekh sakte hain.

---

## Table of Contents
1. [Understand the Build Process (Build Kya Hoti Hai?)](#1-understand-the-build-process)
2. [Step 1: Dockerfile Banana (Multi-stage Build)](#2-step-1-dockerfile-banana-multi-stage-build)
3. [Step 2: Docker Compose File Setup Karna](#3-step-2-docker-compose-file-setup-karna)
4. [Step 3: App Deploy & Run Karna](#4-step-3-app-deploy--run-karna)
5. [Step 4: Domain Mapping (WSL ke sath Domain connect karna)](#5-step-4-domain-mapping-wsl-ke-sath-domain-connect-karna)

---

## 1. Understand the Build Process

**Build kya hoti hai?**
Spring Boot (Java) app ko run karne ke liye hume source code (`.java` files) ko compile karke executable binary format (yaani `.jar` file - Java Archive) mein convert karna hota hai. Is process ko **Build** kehte hain.

### Local Build Test (Optional)
Agar aap local machine par check karna chahte hain ke build kaise banti hai:
1. WSL terminal open karein aur project directory mein jayein.
2. Maven Wrapper use karke build run karein:
   ```bash
   ./mvnw clean package -DskipTests=true
   ```
3. Jab build complete ho jayegi, aapko root folder mein `target/` directory milegi, jisme ek `.jar` file (e.g., `expensesapp-0.0.1-SNAPSHOT.jar`) hogi.
4. Clean karne ke liye aap dobara run kar sakte hain:
   ```bash
   ./mvnw clean
   ```

---

## 2. Step 1: Dockerfile Banana (Multi-stage Build)

Docker mein multi-stage build ka faida ye hota hai ke hamari final Docker image ka size bohot chota ho jata hai. Hum build stage mein heavy tools (jaise Maven) install karte hain, aur runner stage mein sirf lightweight Java Runtime (JRE) use karte hain.

### Kahan banani hai?
Project ke root directory (`\\wsl.localhost\Ubuntu\home\abdullah\projects\Expenses-Tracker-WebApp`) mein ek nayi file banayein jiska naam ho exact: **`Dockerfile`** (bina kisi extension ke).

### Dockerfile ka Content:
Aapko is file mein niche diya gaya code likhna hai:

```dockerfile
# ==========================================
# Stage 1: Build Stage (Maven)
# ==========================================
FROM maven:3.8.3-openjdk-17 AS builder

# Container ke andar workspace directory set karein
WORKDIR /app

# Poora source code (including pom.xml and src/) container mein copy karein
COPY . /app

# Project ko compile aur package (build) karein, tests ko skip karke
RUN mvn clean package -DskipTests=true

# ==========================================
# Stage 2: Final Run Stage (JRE Alpine)
# ==========================================
FROM openjdk:17-alpine

WORKDIR /app

# Stage 1 (builder) se tayyar shuda JAR file ko Stage 2 ke target folder mein copy karein
COPY --from=builder /app/target/*.jar /app/target/expenseapp.jar

# Container ka port 8080 expose karein
EXPOSE 8080

# JAR file ko run karne ki command
ENTRYPOINT ["java", "-jar", "/app/target/expenseapp.jar"]
```

### Explanation of Dockerfile steps:
* `FROM maven:3.8.3-openjdk-17 AS builder`: Yeh build stage hai jisme Maven and Java installed hote hain.
* `COPY . /app`: Hamare source code ko container ke `/app` mein daalta hai.
* `RUN mvn clean package -DskipTests=true`: Jar file generate karta hai.
* `FROM openjdk:17-alpine`: Yeh Alpine Linux image hai jo bohot light-weight hoti hai (around 100MB compared to Maven's 800MB+).
* `COPY --from=builder ...`: Builder stage se jar file copy karta hai runner stage mein.

---

## 3. Step 2: Docker Compose File Setup Karna

Ab hume database (MySQL) aur application (Spring Boot) ko aapas mein link karne ke liye Docker Compose file chahiye takay ek single command se dono service start ho sakein.

### Kahan banani hai?
Project ke root folder mein **`docker-compose.yml`** file banayein.

### docker-compose.yml ka Content:
```yaml
version: "3.8"

services:
  # MySQL Database Service
  mysql:
    image: mysql:8.0
    container_name: mysql_db
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: Test@123
      MYSQL_DATABASE: expenses_tracker
    volumes:
      # Data persist rakhne ke liye volume map karein
      - mysql-data:/var/lib/mysql
    networks:
      - app-network
    # Healthcheck ensure karega ke jab tak MySQL port fully ready nahi hota, tab tak app start na ho
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-pTest@123"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 20s

  # Spring Boot Main Application
  mainapp:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: ExpenseTrackerApp
    restart: always
    environment:
      # Spring Boot application.properties ke database settings ko override karega
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: Test@123
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/expenses_tracker?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
    ports:
      - "8080:8080"
    networks:
      - app-network
    depends_on:
      mysql:
        condition: service_healthy

# Shared Network for containers communication
networks:
  app-network:

# Volume mapping definition
volumes:
  mysql-data:
```

---

## 4. Step 3: App Deploy & Run Karna

Aapki Dockerfile aur docker-compose.yml files ready hain! Ab deploy karne ke liye niche diye gaye steps follow karein:

1. **WSL (Ubuntu) Terminal** mein project path par jayein:
   ```bash
   cd /home/abdullah/projects/Expenses-Tracker-WebApp
   ```
2. **Build and Run Docker containers**:
   ```bash
   docker compose up --build
   ```
   * *Note: `--build` flag har dafa app changes ko rebuild karega.*
3. **Verify Deployment**:
   Containers start hone ke baad database create hoga aur application initialize hogi. Jab log main `Started ExpensesappApplication in ... seconds` show ho jaye, tab aap browser khol kar test kar sakte hain:
   ```
   http://localhost:8080
   ```

---

## 5. Step 4: Domain Mapping (WSL ke sath Domain connect karna)

Agar aapke paas **apni domain** (e.g., `abdullah.com` ya `myproject.myname`) hai aur aap use locally WSL application par map karna chahte hain, toh iske 2 tareeqe hain:

### Option A: Local Domain Mapping (Subse simple practice ke liye)
Yeh tareeqa aapke computer ke andar kaam karega. Jab aap browser mein apni domain likhenge, toh woh automatically aapke local WSL server par point karegi (Bina public internet par expose kiye).

1. Windows mein **Notepad** ko **Run as Administrator** open karein.
2. File -> Open par jayein aur is path par file open karein:
   `C:\Windows\System32\drivers\etc\hosts`
3. File ke aakhir mein niche di gayi line add karein:
   ```text
   127.0.0.1   yourdomain.com
   ```
   *(Apni domain ko `yourdomain.com` ki jagah likhein)*.
4. File save kar dein.
5. Ab aap browser mein jayein aur open karein:
   `http://yourdomain.com:8080`
   Aapki application open ho jayegi!

---

### Option B: Cloudflare Tunnel (Professional / Production level practice with real domain)
Agar aap chahte hain ke aapka domain **real public domain** ban jaye jo internet se access ho sake bina server khareede, toh Cloudflare Tunnel subse behtareen and secure DevOps method hai.

#### Prerequisite:
* Aapka domain Cloudflare DNS par managed hona chahiye (yeh free hai).

#### Steps:
1. **WSL ke andar `cloudflared` install karein**:
   ```bash
   curl -L --output cloudflared.deb https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb
   sudo dpkg -i cloudflared.deb
   ```
2. **Cloudflare Account Login karein**:
   ```bash
   cloudflared tunnel login
   ```
   Terminal mein ek link aayega, uspar click karke browser mein login karein aur apna domain select karke authorize karein.
3. **Tunnel Create karein**:
   ```bash
   cloudflared tunnel create expensetunnel
   ```
   Is command se aapko ek **Tunnel ID** (UUID) milegi.
4. **Domain Route config karein**:
   ```bash
   cloudflared tunnel route dns expensetunnel yourdomain.com
   ```
5. **Tunnel ko run karein**:
   ```bash
   cloudflared tunnel run --url http://localhost:8080 expensetunnel
   ```
6. **Magical Result**:
   Ab pooray internet par kahin se bhi aap `https://yourdomain.com` kholenge, toh traffic secure tunnel ke zariye direct aapke local computer par chal rahe WSL Docker container par aayegi!
