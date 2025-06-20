# Order Payment REST API with RabbitMQ Integration

## 📖 Overview  

This Spring Boot application is designed to handle **Order Payment** operations through a robust and scalable **REST API**, leveraging **RabbitMQ** as the message broker for efficient, asynchronous event communication between services. The system is architected to ensure reliable message delivery and processing for both successful and failed payment scenarios.  

### 🚀 Features  

Below are the core features that make this solution robust and ready for real-world scenarios:  

- **RESTful Order Payment API** – Provides endpoints for initiating and processing order payments.  
- **RabbitMQ Messaging Integration**  
    - Uses `RabbitTemplate` to publish messages to RabbitMQ exchanges upon payment success or failure.  
    - Two primary queues are used: `order.payment.success.queue` and `order.payment.failed.queue`.  
    - Each queue is bound to a dedicated exchange (`order.payment.exchange`) with specific routing keys: `order.payment.success` and `order.payment.failed`.  
    - Utilizes `@RabbitListener` annotated methods to consume and process messages from dedicated queues.  
- **Publisher Retry Support with `RetryTemplate`**  
    - Incorporates `RetryTemplate` in the publishing logic to **re-attempt message sending** in case of transient failures (e.g., broker unavailability).  
    - Ensures robust delivery guarantees by wrapping message publishing logic with retry policies and backoff strategies.  
    - Helps avoid message loss due to temporary network or broker issues, improving resilience on the producer side.  
- **Retry Mechanism (Consumer-Side)**
    - Implements the retry strategy using `RetryInterceptorBuilder`, configured as a **retry advice bean** to handle retries during message consumption.  
    - Applies a **fixed backoff policy**, introducing a configurable delay (e.g., `5 seconds`) between each retry attempt to give transient issues time to resolve.  
    - Allows customization of the **maximum number of retry attempts** (e.g., `3 times`), enabling fine-grained control over retry behavior in case of processing failures.  
    - Uses `RejectAndDontRequeueRecoverer` as the recovery strategy once all retry attempts are exhausted, preventing the message from being requeued and retried indefinitely.  
    - Automatically routes messages that exceed retry attempts to the configured **Dead Letter Queue (DLQ)** (`order.payment.success.dlq` or `order.payment.failed.dlq`), ensuring failed messages are captured for analysis or manual intervention.  
    - Enhances system **resilience and observability** by isolating problematic messages, avoiding message loss, and preventing consumer thread blockage due to persistent failures.  
- **Dead Letter Handling (DLQ)**
    - Both queues `order.payment.success` and `order.payment.failed` are configured with **Dead-Letter Exchanges (DLX) `order.payment.dlx.exchange`** and routing keys to redirect unprocessed or failed messages.  
    - After the maximum number of retry attempts is exceeded, messages are routed to their respective **Dead Letter Queues (DLQs): `order.payment.success.dlq` and `order.payment.failed.dlq`** for further inspection or reprocessing.  


### 🧩 Role of `RejectAndDontRequeueRecoverer`  

In the retry configuration, the project uses the `RejectAndDontRequeueRecoverer` class as the **recoverer strategy**. This class plays a critical role in managing what happens when a message cannot be processed even after the maximum number of retry attempts.  

#### 🎯 Purpose:  

`RejectAndDontRequeueRecoverer` ensures that:  

- The message **is not requeued** to the original queue after the final retry attempt fails.  
- The message is **rejected and marked as dead**, which then triggers the **Dead Letter Exchange (DLX)** logic.  
- This behavior prevents **endless retry loops** and enables the system to move irrecoverable messages to a dedicated **Dead Letter Queue (DLQ)** for later analysis.  

### 🔁 How Message Publishing Work  

1. After a payment is processed (either successfully or with failure), the result is published as a message to **RabbitMQ** using `RabbitTemplate`.  
2. The message is sent to a specific **exchange**, along with a **routing key** that determines which queue will receive the message.  
3. RabbitMQ routes the message based on:  
    - The **type of exchange** (e.g., direct, topic)  
    - The **routing key**  
    - The **bindings** between the exchange and queues  

### 📥 How Message Consumption Works  

1. `@RabbitListener` is used to define **listener methods** that are automatically triggered when a message arrives in a bound queue.  
2. The message is **deserialized** and passed to the method.  
3. Inside the method, application-specific logic is executed (e.g., updating order status, notifying users).  
4. If the method throws an exception, the retry mechanism handles the message according to the retry policy.  

---

## 🤖 Tech Stack  

The technology used in this project are:  

| Technology                   | Description                                                              |
|------------------------------|--------------------------------------------------------------------------|
| **Spring Boot Starter Web**  | For building RESTful APIs and web applications.                          |
| **Spring Boot Starter AMQP** | Integrates RabbitMQ for messaging using `RabbitTemplate`, listeners, etc.|
| **Lombok**                   | Reduces boilerplate code using annotations like `@Getter`, `@Builder`.   |

---

## 🏗️ Project Structure  

The project is organized into the following package structure:  

```bash
order-payment-rabbitmq/
└── src/main/
    ├── 📂docker/
    │   ├── 📂app/                  # Dockerfile untuk application (runtime container)
    │   └── 📂rabbitmq/             # Berisi instruksi build image RabbitMQ dengan konfigurasi custom.
    ├── 📂java/
    │   ├── 📂config/               # All Spring-related configurations: RabbitMQ, retry, listener factory.
    │   ├── 📂controller/           # Defines REST API endpoints for handling order payment requests, acting as the entry point for client interactions.
    │   ├── 📂dto/                  # Contains Data Transfer Objects used for API request and response models, such as creating an order payment.
    │   ├── 📂entity/               # Includes core domain models like Order, OrderDetail, and OrderPayment which represent the message structures.
    │   ├── 📂listener/             # RabbitMQ message consumers for payment success and failure queues.
    │   ├── 📂publisher/            # Components that publish messages to RabbitMQ via `RabbitTemplate`.
    │   ├── 📂recovery/             # Recovery utilities.
    │   ├── 📂service/              # Encapsulates the business logic related to order creation and payment processing.
    │   │   └── 📂impl/             # Implementation of services.
    │   └── 📂util/                 # Helper utilities for transformation or mapping.
    └── 📂resources/                  
        └── application.properties   # Config file (e.g., Application and RabbitMQ configuration)
```
---


## 🛠️ Installation & Setup  

Follow these steps to set up and run the project locally:  

### ✅ Prerequisites

Make sure the following tools are installed on your system:

| Tool                                      | Description                                                   | Required      |
|-------------------------------------------|---------------------------------------------------------------|---------------|
| [Java 17+](https://adoptium.net/)         | Java Development Kit (JDK) to run the Quarkus application     | ✅            |
| [RabbitMQ](https://www.rabbitmq.com/)     | Message broker used for asynchronous communication            | ✅            |
| [Make](https://www.gnu.org/software/make/)| Automation tool for tasks like `make run-app`                 | ✅            |
| [Docker](https://www.docker.com/)         | To run services like RabbitMQ in isolated containers          | ⚠️ *optional* |

### ☕ A. Install Java 17  

1. Ensure **Java 17** is installed on your system. You can verify this with:  

```bash
java --version
```  

2. If Java is not installed, follow one of the methods below based on your operating system:  

#### 🐧 Linux  

**Using apt (Ubuntu/Debian-based)**:  

```bash
sudo apt update
sudo apt install openjdk-17-jdk
```  

#### 🪟 Windows  
1. Use [https://adoptium.net](https://adoptium.net) to download and install **Java 17 (Temurin distribution recommended)**.  

2. After installation, ensure `JAVA_HOME` is set correctly and added to the `PATH`.  

3. You can check this with:  

```bash
echo $JAVA_HOME
```  

### 🐰 B. Set Up RabbitMQ  

1. Install `rabbitmq-server` on Linux  

To install RabbitMQ on a Debian-based system:  

```bash
# Update package list
sudo apt-get update

# Install RabbitMQ
sudo apt-get install rabbitmq-server
```

Then enable and start the service:  

```bash
# Enable RabbitMQ to start on boot
sudo systemctl enable rabbitmq-server

# Start RabbitMQ server immediately
sudo systemctl start rabbitmq-server
```

To ensure that RabbitMQ is running correctly, you can verify the status of the service using the following command:  

```bash
# Check the running status of RabbitMQ service
sudo systemctl status rabbitmq-server
```  

This command will display detailed information about the RabbitMQ service, including whether it's active and running:  

```bash
● rabbitmq-server.service - RabbitMQ Messaging Server
     Loaded: loaded (/usr/lib/systemd/system/rabbitmq-server.service; enabled; preset: enabled)
     Active: active (running) since Wed 2025-04-16 22:09:48 WIB; 12h ago
   Main PID: 164090 (beam.smp)
      Tasks: 84 (limit: 38350)
     Memory: 165.3M ()
     CGroup: /system.slice/rabbitmq-server.service
             ├─164090 /usr/lib/erlang/erts-13.2.2.5/bin/beam.smp -W w -MBas ageffcbf -MHas ageffcbf -MBlmbcs 512 -MHlmbcs 512 -MMmcs 30 -pc unicode -P 1048576 -t 5000000 -stbt db>
             ├─164100 erl_child_setup 65536
             ├─164272 /usr/lib/erlang/erts-13.2.2.5/bin/inet_gethost 4
             ├─164273 /usr/lib/erlang/erts-13.2.2.5/bin/inet_gethost 4
             └─164276 /bin/sh -s rabbit_disk_monitor

Apr 16 22:09:44 LAPTOP-1UDHCSAN systemd[1]: Starting rabbitmq-server.service - RabbitMQ Messaging Server...
Apr 16 22:09:48 LAPTOP-1UDHCSAN systemd[1]: Started rabbitmq-server.service - RabbitMQ Messaging Server.
```  

**📝 Note:** A successful output should show the status as `active (running)`, indicating that RabbitMQ is up and ready to handle messaging operations for the application. If it's not running, you may need to start it with `sudo systemctl start rabbitmq-server`.  

2. Create Dedicated Users  

**Create two users:** one with admin access for UI and another as the regular application user.  

```bash
# Add a user with full access for web-based admin UI
sudo rabbitmqctl add_user spring_admin <password>
# Note: Replace '<password>' with a secure password

# Add a regular user for your Spring Boot application
sudo rabbitmqctl add_user spring_user <password>
# Note: Replace '<password>' with a secure password

# Verifies if the username and password combination is correct
sudo rabbitmqctl authenticate_user <user> <password>
```  

**📝 Note:** The `spring_admin` user is intended for administrative purposes, such as accessing the RabbitMQ Management UI and performing admin-level actions like managing queues, exchanges, and permissions. The `spring_user` is a regular user that your Spring Boot application will use to connect to RabbitMQ and publish/consume messages.  


3. Create Virtual Host (vhost), Assign Permissions, and Tag Users  

Define a virtual host to isolate application-specific messaging.  

```bash
# Create a new virtual host for the order-payment application
sudo rabbitmqctl add_vhost /order-payment

# Lists all virtual hosts on the RabbitMQ server
sudo rabbitmqctl list_vhosts
```  

**📝 Note:** A virtual host in RabbitMQ is like a namespace that provides logical separation between different applications or components using the same RabbitMQ instance. Each vhost can have its own set of queues, exchanges, bindings, and permissions, isolated from others.  

After creating a virtual host, assign appropriate permissions to the users so they can interact with the resources (queues, exchanges, etc.) within that virtual host:  

```bash
# Gives all permissions (configure, write, read) to spring_admin
sudo rabbitmqctl set_permissions -p /order-payment spring_admin ".*" ".*" ".*"

# Gives all permissions to spring_user
sudo rabbitmqctl set_permissions -p /order-payment spring_user ".*" ".*" ".*"

# Lists the permissions of the specified user, to verify permissions and ensure correct access control for each user
sudo rabbitmqctl list_user_permissions <user>
```  

**📝 Note:** In RabbitMQ, permissions are defined with three scopes:  

- **Configure** – Allows the user to create or modify exchanges and queues.  
- **Write** – Allows the user to publish messages to exchanges.  
- **Read** – Allows the user to consume messages from queues and subscribe to queues.  


After creating users, you should assign them appropriate **tags** to define their level of access — especially for web-based management:  

```bash
# Assigns administrator role to spring_admin (needed for management UI access)
sudo rabbitmqctl set_user_tags spring_admin administrator

# Removes all tags from spring_user (default is a regular user)
sudo rabbitmqctl set_user_tags spring_user

# Lists all RabbitMQ users along with their tags, to ensure that the appropriate tags have been successfully assigned to users: spring_admin and spring_user
sudo rabbitmqctl list_users
```  

**📝 Note:** Tags determine what a user can do, particularly through the RabbitMQ Management Web UI. `spring_admin` is tagged as an `administrator` to allow full access to the **Management UI and perform administrative operations**. `spring_user` has no tags, making it a regular messaging user — suitable for interacting with Spring Boot application (publishing/consuming messages).  

4. Enable the Management Plugin  

The RabbitMQ Management Plugin provides a **web-based UI** to monitor, manage, and inspect RabbitMQ components like queues, exchanges, bindings, and messages in real time.  

```bash
# Enables the web UI plugin for RabbitMQ
sudo rabbitmq-plugins enable rabbitmq_management

# Restart the RabbitMQ server
sudo service rabbitmq-server restart
```

Once the plugin is enabled and RabbitMQ restarts successfully, the UI can be accessed from the browser:  

```cpp
http://<your-server-ip>:15672/
```  

**📝 Note:** Use the `spring_admin` user (created earlier) with the assigned `administrator` tag to access this UI. This interface allows administrative actions like creating queues, binding exchanges, inspecting messages, managing users, and more. Ensure that the server port `15672` is open on your firewall or cloud provider (like AWS or DigitalOcean) to access it externally.  

![Image](https://github.com/user-attachments/assets/3679f53a-6b2e-4966-90c7-8534b33cd19d)  

![Image](https://github.com/user-attachments/assets/443b7cd9-05a7-43a9-aef6-9538e9cdc619)  



### 🧰 C. Install `make` (Optional but Recommended)  
This project uses a `Makefile` to streamline common tasks.  

Install `make` if not already available:  

#### 🐧 Linux  

Install `make` using **APT**  

```bash
sudo apt update
sudo apt install make
```  

You can verify installation with:   
```bash
make --version
```  

#### 🪟 Windows  

If you're using **PowerShell**:  

- Install [Chocolatey](https://chocolatey.org/install) (if not installed):  
```bash
Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.SecurityProtocolType]::Tls12; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```  

- Verify `Chocolatey` installation:  
```bash
choco --version
```  

- Install `make` via `Chocolatey`:  
```bash
choco install make
```  

After installation, **restart your terminal** or ensure `make` is available in your `PATH`.  

### 🔁 D. Clone the Project  

Clone the repository:  

```bash
git clone https://github.com/yoanesber/Spring-Boot-Order-Payment-RabbitMQ.git
cd Spring-Boot-Order-Payment-RabbitMQ
```  

### ⚙️ E. Configure Application Properties  

Set up your `application.properties` in `src/main/resources`:  

```properties
# Spring Boot application properties file
spring.application.name=order-payment-rabbitmq
server.port=8080
spring.profiles.active=development

# RabbitMQ configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=spring_user
spring.rabbitmq.password=<password>
spring.rabbitmq.virtual-host=/order-payment
spring.rabbitmq.publisher-returns=true
spring.rabbitmq.channel-cache-size=10
spring.rabbitmq.connection-limit=10
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.requested-heart-beat=30
spring.rabbitmq.connection-timeout=30000

# RabbitMQ exchange and queue configuration
spring.rabbitmq.order-payment.exchange-name=order.payment.exchange
spring.rabbitmq.order-payment.payment-success-queue-name=order.payment.success.queue
spring.rabbitmq.order-payment.payment-failed-queue-name=order.payment.failed.queue
spring.rabbitmq.order-payment.payment-success-routing-key=order.payment.success
spring.rabbitmq.order-payment.payment-failed-routing-key=order.payment.failed

# RabbitMQ dead-letter exchange and queue configuration
spring.rabbitmq.order-payment.exchange-dlx-name=order.payment.dlx.exchange
spring.rabbitmq.order-payment.dlq-success-queue-name=order.payment.success.dlq
spring.rabbitmq.order-payment.dlq-failed-queue-name=order.payment.failed.dlq
spring.rabbitmq.order-payment.dlq-success-routing-key=order.payment.success.dlq
spring.rabbitmq.order-payment.dlq-failed-routing-key=order.payment.failed.dlq
```

**📌 Note:** Replace host and credentials with actual values for your environment.

## 🚀 F. Running the Application  

This section provides step-by-step instructions to run the application either **locally** or via **Docker containers**.

- **Notes**:  
  - All commands are defined in the `Makefile`.
  - To run using `make`, ensure that `make` is installed on your system.
  - To run the application in containers, make sure `Docker` is installed and running.

### 🔧 Run Locally (Non-containerized)

Ensure RabbitMQ Messaging Server are running locally, then:

```bash
make dev
```

### 🐳 Run Using Docker

To build and run all services (RabbitMQ Messaging Server, Spring app):

```bash
make docker-start-all
```

To stop and remove all containers:

```bash
make docker-stop-all
```

- **Notes**:  
  - Before running the application inside Docker, make sure to update your `application.properties`
    - Replace `localhost` with the appropriate **container name** for services like RabbitMQ Messaging Server.  
    - For example:
      - Change `localhost` to `rabbitmq-server`

### 🟢 Application is Running

Now your application is accessible at:
```bash
http://localhost:8080
```

---


## 🧪 Testing Scenarios

In this Order Payment REST API project, backed by RabbitMQ, involves a critical messaging flow between publisher and consumer with retry and dead-letter handling. Proper testing ensures the reliability, robustness, and maintainability of the system. Below are categorized test scenarios complete with context and visual aid placement suggestions.  

### A. REST API Testing  

The REST API serves as the entry point for order payment creation. These endpoints must be validated against both valid and invalid inputs, ensuring correct status codes and responses are returned. Negative tests help ensure that error handling is consistent and secure.  

1. **✅ Test Case: Create Order with Valid Payload**  

Create order with valid payload → expect `201 Created`.  

**Endpoint:**  

```bash
POST http://localhost:8080/api/v1/order-payment
Content-Type: application/json
```  

**Request:**  

```json
{
    "orderId":"ORD123456789",
    "amount":"199.99",
    "currency":"USD",
    "paymentMethod":"CREDIT_CARD",
    "cardNumber":"1234 5678 9012 3456",
    "cardExpiry":"31/12",
    "cardCvv":"123"
}
```

**Expected Result:** `201 Created`  

📸 Postman screenshot for successful creation  

![Image](https://github.com/user-attachments/assets/004e9afa-c558-4049-9955-8ff559913772)  

2. **❌ Test Case: Missing Field (e.g., no paymentMethod)**  

Missing or invalid fields → expect `400 Bad Request`.  

**Request:**  

```json
{
    "orderId":"ORD123456789",
    "amount":"199.99",
    "currency":"USD",
    "cardNumber":"1234 5678 9012 3456",
    "cardExpiry":"31/12",
    "cardCvv":"123"
}
```

**Expected Result:** `400 Bad Request`  

📸 Postman screenshot showing validation error  

![Image](https://github.com/user-attachments/assets/53869e34-f85d-4866-b35f-d146b4e0a902)  

### B. RabbitMQ Publisher Testing  

The RabbitMQ publisher is responsible for sending serialized JSON messages to the `order.payment.success.queue` or `order.payment.failed.queue` queues in `order.payment.exchange` exchanges. Tests should verify correct exchange/routing key usage, message structure, and RabbitTemplate confirm callbacks.  

1. **✅ Test Case: Message Published Successfully**  

Successful publish logs and confirms `ack=true`.  

**Published Payload:**  
```json
{
  "id": 1744814928936,
  "orderId": "ORD123456789",
  "amount": 199.99,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD",
  "paymentStatus": "SUCCESS",
  "cardNumber": "1234 5678 9012 3456",
  "cardExpiry": "31/12",
  "cardCvv": "123",
  "paypalEmail": null,
  "bankAccount": null,
  "bankName": null,
  "transactionId": "TXN1744814928936",
  "retryCount": 0,
  "createdAt": 1744814928.9367597,
  "updatedAt": 1744814928.9367597
}
```  

**Expected Result:** `ack=true` from `ConfirmCallback`  

📸 Log output  

```bash
2025-04-16T21:48:48.944+07:00  INFO 20712 --- [order-payment-rabbitmq] [ntContainer#1-1] c.y.o.listener.PaymentListener           : Processing message for the first time. Message: {id=1744814928936, orderId=ORD123456789, amount=199.99, currency=USD, paymentMethod=CREDIT_CARD, paymentStatus=SUCCESS, cardNumber=1234 5678 9012 3456, cardExpiry=31/12, cardCvv=123, paypalEmail=null, bankAccount=null, bankName=null, transactionId=TXN1744814928936, retryCount=0, createdAt=1.7448149289367597E9, updatedAt=1.7448149289367597E9}
2025-04-16T21:48:48.950+07:00  INFO 20712 --- [order-payment-rabbitmq] [nectionFactory5] c.y.o.c.RabbitMQConfig$$SpringCGLIB$$0   : RabbitMQ message acknowledged: null, ack: true, cause: null
```  

**📝 Note:** `ack=true` in the `ConfirmCallback` means that the message has been **successfully received by the broker's exchange**, **not** necessarily consumed by the queue or processed by the listener. This confirmation only applies to the **publisher's side** (i.e., the `RabbitTemplate`) and indicates that RabbitMQ has **acknowledged receipt** of the message for routing.  

2. **❌ Test Case: RabbitMQ Not Reachable**  

Simulate RabbitMQ being offline when sending a payment message using `RabbitTemplate` (e.g., stopping the RabbitMQ service, misconfiguring the port, or pointing to a non-existent host in `application.properties`).  

**Published Payload:** (Same as the valid payload from the previous test case)  

**Expected Result:**  

- `RabbitTemplate.convertAndSend(...)` throws an exception — specifically `AmqpConnectException`.  
- No `ack=false` will be triggered from `ConfirmCallback`, because the publisher was **never able to establish a connection** with the RabbitMQ broker in the first place.  
- Since the message never leaves the application, RabbitMQ has no awareness of the message — hence **acknowledgment (ACK) callbacks are never invoked**.  
- Proper **error-handling logic** should **log** the error and potentially trigger **alerts**, **fallbacks**, or **message persistence** for retry later.  

📸 Log output showing publishing failure  

```bash
2025-04-17T12:10:03.110+07:00  INFO 51272 --- [order-payment-rabbitmq] [nio-8080-exec-3] c.y.o.publisher.MessagePublisher         : Attempt 1 to publish message: OrderPayment(id=1744866603110, orderId=ORD123456789, amount=199.99, currency=USD, paymentMethod=CREDIT_CARD, paymentStatus=SUCCESS, cardNumber=1234 5678 9012 3456, cardExpiry=31/12, cardCvv=123, paypalEmail=null, bankAccount=null, bankName=null, transactionId=TXN1744866603110, retryCount=0, createdAt=2025-04-17T05:10:03.110109600Z, updatedAt=2025-04-17T05:10:03.110109600Z)
2025-04-17T12:10:04.895+07:00 ERROR 51272 --- [order-payment-rabbitmq] [ntContainer#0-5] o.s.a.r.l.SimpleMessageListenerContainer : Failed to check/redeclare auto-delete queue(s).
2025-04-17T12:10:04.895+07:00  INFO 51272 --- [order-payment-rabbitmq] [nio-8080-exec-3] o.s.a.r.c.CachingConnectionFactory       : Attempting to connect to: 172.26.161.183:5672
2025-04-17T12:10:06.968+07:00  INFO 51272 --- [order-payment-rabbitmq] [ntContainer#0-5] o.s.a.r.c.CachingConnectionFactory       : Attempting to connect to: 172.26.161.183:5672
2025-04-17T12:10:07.919+07:00  WARN 51272 --- [order-payment-rabbitmq] [ntContainer#1-4] o.s.a.r.l.SimpleMessageListenerContainer : Consumer raised exception, processing can restart if the connection factory supports it. Exception summary: org.springframework.amqp.AmqpConnectException: java.net.ConnectException: Connection refused: getsockopt
2025-04-17T12:10:07.920+07:00  INFO 51272 --- [order-payment-rabbitmq] [ntContainer#1-4] o.s.a.r.l.SimpleMessageListenerContainer : Restarting Consumer@6ff03037: tags=[[]], channel=null, acknowledgeMode=AUTO local queue size=0
2025-04-17T12:10:08.976+07:00  INFO 51272 --- [order-payment-rabbitmq] [nio-8080-exec-3] c.y.o.publisher.MessagePublisher         : Attempt 2 to publish message: OrderPayment(id=1744866603110, orderId=ORD123456789, amount=199.99, currency=USD, paymentMethod=CREDIT_CARD, paymentStatus=SUCCESS, cardNumber=1234 5678 9012 3456, cardExpiry=31/12, cardCvv=123, paypalEmail=null, bankAccount=null, bankName=null, transactionId=TXN1744866603110, retryCount=0, createdAt=2025-04-17T05:10:03.110109600Z, updatedAt=2025-04-17T05:10:03.110109600Z)
2025-04-17T12:10:09.039+07:00  INFO 51272 --- [order-payment-rabbitmq] [ntContainer#1-5] o.s.a.r.c.CachingConnectionFactory       : Attempting to connect to: 172.26.161.183:5672
2025-04-17T12:10:11.113+07:00 ERROR 51272 --- [order-payment-rabbitmq] [ntContainer#1-5] o.s.a.r.l.SimpleMessageListenerContainer : Failed to check/redeclare auto-delete queue(s).
2025-04-17T12:10:11.113+07:00  INFO 51272 --- [order-payment-rabbitmq] [nio-8080-exec-3] o.s.a.r.c.CachingConnectionFactory       : Attempting to connect to: 172.26.161.183:5672
2025-04-17T12:10:13.170+07:00  INFO 51272 --- [order-payment-rabbitmq] [ntContainer#1-5] o.s.a.r.c.CachingConnectionFactory       : Attempting to connect to: 172.26.161.183:5672
2025-04-17T12:10:14.113+07:00  WARN 51272 --- [order-payment-rabbitmq] [ntContainer#0-5] o.s.a.r.l.SimpleMessageListenerContainer : Consumer raised exception, processing can restart if the connection factory supports it. Exception summary: org.springframework.amqp.AmqpConnectException: java.net.ConnectException: Connection refused: getsockopt
2025-04-17T12:10:14.113+07:00  INFO 51272 --- [order-payment-rabbitmq] [ntContainer#0-5] o.s.a.r.l.SimpleMessageListenerContainer : Restarting Consumer@4b2e39b5: tags=[[]], channel=null, acknowledgeMode=AUTO local queue size=0
2025-04-17T12:10:15.173+07:00  INFO 51272 --- [order-payment-rabbitmq] [nio-8080-exec-3] c.y.o.publisher.MessagePublisher         : Attempt 3 to publish message: OrderPayment(id=1744866603110, orderId=ORD123456789, amount=199.99, currency=USD, paymentMethod=CREDIT_CARD, paymentStatus=SUCCESS, cardNumber=1234 5678 9012 3456, cardExpiry=31/12, cardCvv=123, paypalEmail=null, bankAccount=null, bankName=null, transactionId=TXN1744866603110, retryCount=0, createdAt=2025-04-17T05:10:03.110109600Z, updatedAt=2025-04-17T05:10:03.110109600Z)
2025-04-17T12:10:15.222+07:00  INFO 51272 --- [order-payment-rabbitmq] [ntContainer#0-6] o.s.a.r.c.CachingConnectionFactory       : Attempting to connect to: 172.26.161.183:5672
2025-04-17T12:10:17.282+07:00 ERROR 51272 --- [order-payment-rabbitmq] [ntContainer#0-6] o.s.a.r.l.SimpleMessageListenerContainer : Failed to check/redeclare auto-delete queue(s).
2025-04-17T12:10:17.282+07:00  INFO 51272 --- [order-payment-rabbitmq] [nio-8080-exec-3] o.s.a.r.c.CachingConnectionFactory       : Attempting to connect to: 172.26.161.183:5672
2025-04-17T12:10:19.328+07:00 ERROR 51272 --- [order-payment-rabbitmq] [nio-8080-exec-3] c.y.o.publisher.MessagePublisher         : All retry attempts failed to publish message: OrderPayment(id=1744866603110, orderId=ORD123456789, amount=199.99, currency=USD, paymentMethod=CREDIT_CARD, paymentStatus=SUCCESS, cardNumber=1234 5678 9012 3456, cardExpiry=31/12, cardCvv=123, paypalEmail=null, bankAccount=null, bankName=null, transactionId=TXN1744866603110, retryCount=0, createdAt=2025-04-17T05:10:03.110109600Z, updatedAt=2025-04-17T05:10:03.110109600Z). Last error: java.net.ConnectException: Connection refused: getsockopt
...
```  

**📝 Note:** The RabbitMQ broker is offline or unreachable (`Connection refused`), so the application is trying to **retry sending the message 3 times** before giving up. If the RabbitMQ broker is unreachable, the message is never delivered, and therefore **no ACK/NACK is received**, and the **callback is not triggered at all**. The `ConfirmCallback` and its `ack=true/false` behavior **only applies** when a message has successfully reached the **RabbitMQ broker** and is either accepted or rejected at the **exchange level**.  

### C. RabbitMQ Consumer/Listener Testing  

The RabbitMQ listener consumes payment messages from `order.payment.success.queue` or `order.payment.failed.queue`. These must be processed correctly or retried on failure, with DLQ routing as fallback.  

1. **✅ Test Case: Listener Processes Successfully**  

Message successfully processed by listener.  

**Expected Result:** Message processed  

📸 RabbitMQ UI: view of `order.payment.success.queue` or `order.payment.failed.queue`  

![Image](https://github.com/user-attachments/assets/a3816bb6-03b7-4e5e-99e4-e73b46463ba3)

**📝 Note:** “Message processed” – messages that have been successfully processed by a consumer (i.e., `ack=true`) are removed from the queue and no longer visible in the RabbitMQ Management UI.  

📸 Log output of processing pipeline  

```bash
2025-04-16T22:32:15.560+07:00  INFO 27152 --- [order-payment-rabbitmq] [nectionFactory2] c.y.o.c.RabbitMQConfig$$SpringCGLIB$$0   : RabbitMQ message acknowledged: null, ack: true, cause: null
2025-04-16T22:32:15.683+07:00  INFO 27152 --- [order-payment-rabbitmq] [ntContainer#1-1] c.y.o.listener.PaymentListener           : Processing message for the first time. Message: {id=1744817535436, orderId=ORD123456789, amount=199.99, currency=USD, paymentMethod=CREDIT_CARD, paymentStatus=SUCCESS, cardNumber=1234 5678 9012 3456, cardExpiry=31/12, cardCvv=123, paypalEmail=null, bankAccount=null, bankName=null, transactionId=TXN1744817535436, retryCount=0, createdAt=1.7448175354363935E9, updatedAt=1.7448175354363935E9}
```  

**📝 Note:** `@RabbitListener` received the message and processed it. It confirms the message went through the whole pipeline: *deserialization → validation → business logic → ack*.  


2. **❌ Test Case: Forced Exception → Retry**  

Forced exception triggers retry (maxAttempts reached). This project uses `RetryInterceptorBuilder` with a fixed delay and maximum retry attempts. If all attempts fail, the `RejectAndDontRequeueRecoverer` ensures the message is discarded or routed to DLQ without being requeued (does not enter infinite loops).  

**Consumed Message (from Queue):**  

```json
{
  "id": 1744818926645,
  "orderId": "ORD123456789",
  "amount": 199.99,
  "currency": "USD",
  "paymentMethod": "CREDIT_CARD",
  "paymentStatus": "SUCCESS",
  "cardNumber": "1234 5678 9012 3456",
  "cardExpiry": "31/12",
  "cardCvv": "123",
  "paypalEmail": null,
  "bankAccount": null,
  "bankName": null,
  "transactionId": "TXN1744818926644",
  "retryCount": 0,
  "createdAt": 1744818926.645309100,
  "updatedAt": 1744818926.645309100
}
```

**Expected Result:** 3 attempts (or configured max), then DLQ  

📸 Code snippet of retry config  

```java
public class RetryConfig {

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_INTERVAL = 5000L; // 5 second
    private static final double MULTIPLIER = 1.0; // No multiplier, fixed interval
    private static final long MAX_INTERVAL = 10000L; // 10 seconds

    @Bean
    public Advice successQueueRetryAdvice() {
        return RetryInterceptorBuilder.stateless()
            .maxAttempts(MAX_ATTEMPTS)
            .backOffOptions(INITIAL_INTERVAL, MULTIPLIER, MAX_INTERVAL)
            .recoverer(new LoggingRejectAndDontRequeueRecoverer())
            .build();
    }

    @Bean
    public Advice failedQueueRetryAdvice() {
        return RetryInterceptorBuilder.stateless()
            .maxAttempts(MAX_ATTEMPTS)
            .backOffOptions(INITIAL_INTERVAL, MULTIPLIER, MAX_INTERVAL)
            .recoverer(new LoggingRejectAndDontRequeueRecoverer())
            .build();
    }
}
```

📸 Code snippet of **Forced Exception**  

```java
@Component
public class PaymentListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final boolean isSimulated = true; // Simulate processing failure for demonstration purposes 

    @RabbitListener(queues = "order.payment.success.queue", containerFactory = "successQueueFactory")
    public void handleSuccess(Message message) throws Exception {
        // Get the message body as a Map
        Map<String, Object> messageMap = HelperUtil.convertToMap(message.getBody());
        
        // Get the retry count from the RetrySynchronizationManager
        int retryCount = Optional.ofNullable(RetrySynchronizationManager.getContext())
                             .map(RetryContext::getRetryCount)
                             .orElse(0);

        if (retryCount > 0) {
            logger.info("Retrying message processing. Retry count: {} with message: {}", retryCount, messageMap.toString());
        } else {
            logger.info("Processing message for the first time. Message: {}", messageMap.toString());
        }

        // Process the message
        // For example, update the order status in the database or send a notification

        // Simulate processing failure for demonstration purposes
        if (isSimulated) {
            logger.info("Simulating processing failure for demonstration purposes...");
            throw new RuntimeException("Simulated processing failure...");
        }
    }

    @RabbitListener(queues = "order.payment.failed.queue", containerFactory = "failedQueueFactory")
    public void handleFailed(Message message) throws Exception {
        // Get the message body as a Map
        Map<String, Object> messageMap = HelperUtil.convertToMap(message.getBody());
        
        // Get the retry count from the RetrySynchronizationManager
        int retryCount = Optional.ofNullable(RetrySynchronizationManager.getContext())
                             .map(RetryContext::getRetryCount)
                             .orElse(0);

        if (retryCount > 0) {
            logger.info("Retrying message processing. Retry count: {} with message: {}", retryCount, messageMap.toString());
        } else {
            logger.info("Processing message for the first time. Message: {}", messageMap.toString());
        }

        // Process the message
        // For example, log the error or send an alert

        // Simulate processing failure for demonstration purposes
        if (isSimulated) {
            logger.info("Simulating processing failure for demonstration purposes...");
            throw new RuntimeException("Simulated processing failure...");
        }
    }
}
```

📸 RabbitMQ UI with retries and DLQ  
Showing the `order.payment.success.dlq` queue 
![Image](https://github.com/user-attachments/assets/f66513ed-0ace-43c3-a17b-9a4fc4091ea8)  

and the message details in it: 
![Image](https://github.com/user-attachments/assets/fd2df350-0543-49a9-8e5d-76aaf1baae86)  

📸 Log showing retries and recoverer invoked  

```bash
2025-04-16T22:55:26.877+07:00  INFO 50532 --- [order-payment-rabbitmq] [nectionFactory2] c.y.o.c.RabbitMQConfig$$SpringCGLIB$$0   : RabbitMQ message acknowledged: null, ack: true, cause: null
2025-04-16T22:55:27.055+07:00  INFO 50532 --- [order-payment-rabbitmq] [ntContainer#1-1] c.y.o.listener.PaymentListener           : Processing message for the first time. Message: {id=1744818926645, orderId=ORD123456789, amount=199.99, currency=USD, paymentMethod=CREDIT_CARD, paymentStatus=SUCCESS, cardNumber=1234 5678 9012 3456, cardExpiry=31/12, cardCvv=123, paypalEmail=null, bankAccount=null, bankName=null, transactionId=TXN1744818926644, retryCount=0, createdAt=1.7448189266453092E9, updatedAt=1.7448189266453092E9}
2025-04-16T22:55:27.063+07:00  INFO 50532 --- [order-payment-rabbitmq] [ntContainer#1-1] c.y.o.listener.PaymentListener           : Simulating processing failure for demonstration purposes...
2025-04-16T22:55:32.075+07:00  INFO 50532 --- [order-payment-rabbitmq] [ntContainer#1-1] c.y.o.listener.PaymentListener           : Retrying message processing. Retry count: 1 with message: {id=1744818926645, orderId=ORD123456789, amount=199.99, currency=USD, paymentMethod=CREDIT_CARD, paymentStatus=SUCCESS, cardNumber=1234 5678 9012 3456, cardExpiry=31/12, cardCvv=123, paypalEmail=null, bankAccount=null, bankName=null, transactionId=TXN1744818926644, retryCount=0, createdAt=1.7448189266453092E9, updatedAt=1.7448189266453092E9}
2025-04-16T22:55:32.075+07:00  INFO 50532 --- [order-payment-rabbitmq] [ntContainer#1-1] c.y.o.listener.PaymentListener           : Simulating processing failure for demonstration purposes...
2025-04-16T22:55:37.078+07:00  INFO 50532 --- [order-payment-rabbitmq] [ntContainer#1-1] c.y.o.listener.PaymentListener           : Retrying message processing. Retry count: 2 with message: {id=1744818926645, orderId=ORD123456789, amount=199.99, currency=USD, paymentMethod=CREDIT_CARD, paymentStatus=SUCCESS, cardNumber=1234 5678 9012 3456, cardExpiry=31/12, cardCvv=123, paypalEmail=null, bankAccount=null, bankName=null, transactionId=TXN1744818926644, retryCount=0, createdAt=1.7448189266453092E9, updatedAt=1.7448189266453092E9}
2025-04-16T22:55:37.080+07:00  INFO 50532 --- [order-payment-rabbitmq] [ntContainer#1-1] c.y.o.listener.PaymentListener           : Simulating processing failure for demonstration purposes...
2025-04-16T22:55:37.082+07:00 ERROR 50532 --- [order-payment-rabbitmq] [ntContainer#1-1] o.r.LoggingRejectAndDontRequeueRecoverer : Message recovery invoked after retries exhausted.
2025-04-16T22:55:37.082+07:00 ERROR 50532 --- [order-payment-rabbitmq] [ntContainer#1-1] o.r.LoggingRejectAndDontRequeueRecoverer : Retry Count 3: Max retries reached.
2025-04-16T22:55:37.082+07:00 ERROR 50532 --- [order-payment-rabbitmq] [ntContainer#1-1] o.r.LoggingRejectAndDontRequeueRecoverer : Message: {"id":1744818926645,"orderId":"ORD123456789","amount":199.99,"currency":"USD","paymentMethod":"CREDIT_CARD","paymentStatus":"SUCCESS","cardNumber":"1234 5678 9012 3456","cardExpiry":"31/12","cardCvv":"123","paypalEmail":null,"bankAccount":null,"bankName":null,"transactionId":"TXN1744818926644","retryCount":0,"createdAt":1744818926.645309100,"updatedAt":1744818926.645309100}
2025-04-16T22:55:37.082+07:00 ERROR 50532 --- [order-payment-rabbitmq] [ntContainer#1-1] o.r.LoggingRejectAndDontRequeueRecoverer : Cause: Listener method 'public void com.yoanesber.order_payment_rabbitmq.listener.PaymentListener.handleSuccess(org.springframework.amqp.core.Message) throws java.lang.Exception' threw exception
2025-04-16T22:55:37.101+07:00  WARN 50532 --- [order-payment-rabbitmq] [ntContainer#1-1] o.r.LoggingRejectAndDontRequeueRecoverer : Retries exhausted for message (Body:'[B@44d08ca5(byte[376])' MessageProperties [headers={spring_listener_return_correlation=0cb75295-9e3f-4efc-91d1-65c084a55c38, __TypeId__=com.yoanesber.order_payment_rabbitmq.entity.OrderPayment}, contentType=application/json, contentEncoding=UTF-8, contentLength=0, receivedDeliveryMode=PERSISTENT, priority=0, redelivered=false, receivedExchange=order.payment.exchange, receivedRoutingKey=order.payment.success, deliveryTag=1, consumerTag=amq.ctag-OcdL4nJY1xfzNRPBBZShCg, consumerQueue=order.payment.success.queue])

org.springframework.amqp.rabbit.support.ListenerExecutionFailedException: Listener method 'public void com.yoanesber.order_payment_rabbitmq.listener.PaymentListener.handleSuccess(org.springframework.amqp.core.Message) throws java.lang.Exception' threw exception
...
```

**📝 Note:** The test case successfully demonstrates the **retry** and **dead-letter queue (DLQ)** mechanism in a RabbitMQ message consumer. A forced exception in the listener triggered the retry logic, configured with a maximum of `3` attempts and a fixed delay. As expected, the message failed during all retry attempts, and the `RejectAndDontRequeueRecoverer` was invoked to prevent the message from being requeued. Consequently, the unprocessable message was routed to the `order.payment.success.dlq` queue, confirming that the system correctly handles retry exhaustion and prevents infinite processing loops. Log outputs and the RabbitMQ UI both validate this behavior.  


### D. Performance & Load Testing  

Validate how well the system handles high-volume requests and message bursts. This testing reveals bottlenecks in publishing, queue processing.  

Each test case in the performance testing scenarios uses a consistent JSON payload to simulate order payment requests. The payload includes an `orderId` field that dynamically changes with each iteration using the pattern `ORD${i}`, where `${i}` represents the loop index or unique counter per request. The full request body contains static values for amount, currency, and payment details, such as card number, expiry date, and CVV, structured as follows:  

**Payload:**  
```json
{
    "orderId":"ORD${i}",
    "amount":"199.99",
    "currency":"USD",
    "paymentMethod":"CREDIT_CARD",
    "cardNumber":"1234 5678 9012 3456",
    "cardExpiry":"31/12",
    "cardCvv":"123"
}
```  


1. **✅ Test Case: Load Test (Normal Load Testing)**  

To simulate regular user traffic where users arrive gradually, similar to typical daily usage.  

| Parameter             | Value                                    |
|-----------------------|------------------------------------------|
| **Number of Threads** | 100 users                                |
| **Ramp-up Period**    | 100 seconds *(1 user per second)*        |
| **Loop Count**        | 1 *(based on average orders per user)*   |

🎯 Why?  

- Ideal for simulating normal usage patterns.  
- Useful for observing the API’s stability and message publishing behavior to RabbitMQ under typical load.  

📸 Graph from performance testing tool: JMeter  

### 📋 JMeter Summary Report Details  

| **Field**                 | **Value**           | **Explanation**                                                                                                                                             |
|---------------------------|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Label**                 | HTTP Request        | The name of the request sampler.                                                                                                                            |
| **Samples**               | 100                 | Total number of requests sent.                                                                                                                              |
| **Average (ms)**          | 2017                | Average response time: The average time each request took to respond in milliseconds.                                                                       |
| **Min (ms)**              | 2007                | The fastest recorded response time in milliseconds.                                                                                                         |
| **Max (ms)**              | 2037                | The slowest recorded response time in milliseconds.                                                                                                         |
| **Std. Dev.**             | 6.048               | Standard deviation in milliseconds – how much the response times vary from the average. Here it’s 6.05 ms, which means the requests are very consistent.    |
| **Error %**               | 0.0%                | The percentage of failed requests – 0.0% means all requests were successful.                                                                                |
| **Throughput (req/sec)**  | 0.9902              | Number of requests per second – about 0.99 requests/sec, so almost 1 request per second was processed.                                                      |
| **Received KB/sec**       | 0.4636              | The amount of data received from the server per second in kilobytes.                                                                                        |
| **Sent KB/sec**           | 0.3587              | The amount of data sent to the server per second in kilobytes.                                                                                              |
| **Avg. Bytes**            | 479.41              | The average size of each response in bytes.                                                                                                                 |

#### 📌 Key Insights  

- API is consistent (low Std. Dev) and stable (0% error).
- Response time is ~2 seconds per request – acceptable or not depends on Business SLAs.
- Throughput is just under 1 req/sec — this is based on how the test was configured (possibly 1 user/sec ramp-up).


📸 Message rates from RabbitMQ UI  

![Image](https://github.com/user-attachments/assets/79d0a093-4513-488f-bbd4-f4ccf46c7bb9)

2. **✅ Test Case: Spike Test (Sudden Load Testing)**  

To evaluate how the system handles sudden user spikes, such as during a flash sale or promo event.  

| Parameter             | Value                                     |
|-----------------------|-------------------------------------------|
| **Number of Threads** | 1000 users                                |
| **Ramp-up Period**    | 0 seconds *(all users at once)*           |
| **Loop Count**        | 1 *(based on average orders per user)*    |

🎯 Why?  

- Helps determine if the API and RabbitMQ producer can remain stable when hit with a massive surge of requests.  
- Great for checking queue pressure in RabbitMQ and identifying potential bottlenecks.  

📸 Graph from performance testing tool: JMeter  

### 📋 JMeter Summary Report Details  

| **Field**           | **Value**          | **Explanation**                                                                                                    |
|---------------------|--------------------|--------------------------------------------------------------------------------------------------------------------|
| **Label**           | HTTP Request       | The name of the request sampler.                                                                                   |
| **Samples**         | 1000               | Total number of requests sent: `1000`.                                                                             |
| **Avg (ms)**        | 5269               | Average response time: `5269` ms (each request took ~`5.3` seconds on average).                                    |
| **Min (ms)**        | 2005               | The fastest recorded response time: `2005` ms.                                                                     |
| **Max (ms)**        | 8553               | The slowest recorded response time: `8553` ms.                                                                     |
| **Std. Dev.**       | 2277.44            | Standard deviation: `2277.44 ms`. Indicates high variation — some responses were significantly faster or slower.   |
| **Error %**         | 0.0%               | All requests were successful (no errors occurred).                                                                 |
| **Throughput**      | 96.86 req/sec      | The system processed ~`96.86 req/sec` requests per second – showing high performance throughput.                   |
| **Received KB/sec** | 45.45 KB/sec       | Data received per second from the server.                                                                          |
| **Sent KB/sec**     | 35.18 KB/sec       | Data sent per second to the server.                                                                                |
| **Avg. Bytes**      | 480.44 bytes       | Average size of each response.                                                                                     |


#### 📌 Key Insights  

- High average response time (`5269 ms`) suggests potential processing delay.  
- High Std. Dev. (`2277 ms`) indicates performance inconsistency — the API's response time varies significantly under load.  
- Throughput is strong (`96.86 req/sec`), showing the system can process a lot of traffic.  
- No errors occurred, which is a good sign of stability.  

📸 Message rates from RabbitMQ UI  

![Image](https://github.com/user-attachments/assets/5ea2cbcd-f41f-4792-9dc8-517eaa5ae7cb)  

3. **✅ Test Case: Stress Test (Maximum Capacity Testing)**  

To push the system to its maximum limit and discover its failure point.  

| Parameter             | Value                                |
|-----------------------|--------------------------------------|
| **Number of Threads** | 500 users                            |
| **Ramp-up Period**    | 10 seconds *(fast, but not instant)* |
| **Loop Count**        | 10 or until system breaks            |

🎯 Why?  

- Forces the system to operate under pressure beyond expected levels.  
- Helps identify memory leaks, RabbitMQ queue saturation, or API/server crashes.  
- Useful for validating auto-scaling behavior if the system supports it.  

📸 Graph from performance testing tool: JMeter  

### 📋 JMeter Summary Report Details  

| **Field**           | **Value**          | **Explanation**                                                                                    |
|---------------------|--------------------|----------------------------------------------------------------------------------------------------|
| **Label**           | HTTP Request       | The name of the request sampler.                                                                   |
| **Samples**         | 5000               | Total number of requests sent: `5000`.                                                             |
| **Avg (ms)**        | 4310               | Average response time: `4310 ms` (each request took ~`4.3` seconds on average).                    |
| **Min (ms)**        | 2001               | The fastest recorded response time: `2001` ms.                                                     |
| **Max (ms)**        | 5069               | The slowest recorded response time: `5069` ms.                                                     |
| **Std. Dev.**       | 1035.69            | Standard deviation: `1035.69 ms` – shows moderate variability in response time.                    |
| **Error %**         | 0.0%               | All requests were successful (no errors occurred).                                                 |
| **Throughput**      | 93.56 req/sec      | The system handled about ~`93.56` requests per second – indicating solid performance under load.   |
| **Received KB/sec** | 43.97 KB/sec       | Amount of data received per second from the server.                                                |
| **Sent KB/sec**     | 34.06 KB/sec       | Amount of data sent per second to the server.                                                      |
| **Avg. Bytes**      | 481.22 bytes       | Average size of each response.                                                                     |

#### 📌 Key Insights  

- The system maintained `0%` error rate while processing `5000` requests.  
- Response times were consistent, though not extremely tight – showing moderate deviation.  
- Throughput near `94 req/sec` is strong and indicates the system is handling traffic efficiently.  

📸 Message rates from RabbitMQ UI  

![Image](https://github.com/user-attachments/assets/aeecbe08-7c1a-4695-8d83-5524ba563d7b)  

---


## 🔗 Related Repositories  

- For the Redis Stream as Message Producer implementation, check out [Order Payment Service with Redis Streams as Reliable Message Producer for PAYMENT_SUCCESS / PAYMENT_FAILED Events](https://github.com/yoanesber/Spring-Boot-Redis-Stream-Producer).  
- For the Redis Stream as Message Consumer implementation, check out [Spring Boot Redis Stream Consumer with ThreadPoolTaskScheduler Integration](https://github.com/yoanesber/Spring-Boot-Redis-Stream-Consumer).  
- For the Redis Publisher implementation, check out [Spring Boot Redis Publisher with Lettuce](https://github.com/yoanesber/Spring-Boot-Redis-Publisher-Lettuce).  
- For the Redis Subscriber implementation, check out [Spring Boot Redis Subscriber with Lettuce](https://github.com/yoanesber/Spring-Boot-Redis-Subscriber-Lettuce).  