# NexChat

A Real-Time Client-Server Chat Application built with pure Java TCP Sockets and Swing GUI. This project is developed as part of the Network Programming course (3rd Year IT Dept, Borg Al Arab Technological University).

![NexChat Preview](https://via.placeholder.com/1000x680/1A1D2E/FFFFFF?text=NexChat+GUI)

## 🏗️ System Architecture

The application follows a robust Client-Server architecture designed for concurrency and responsiveness:

*   **Server (`server` package):**
    *   Handles multiple concurrent client connections using a multithreaded approach (`ServerSocket` with a dedicated `Thread` per `ClientHandler`).
    *   Manages a thread-safe `UsersRegistry` (backed by `ConcurrentHashMap`) to keep track of active connections, prevent duplicate usernames, and broadcast messages to all users.
*   **Client (`client` & `ui` packages):**
    *   **GUI:** Built with Java Swing, featuring a modern 3-panel dark-themed interface, custom message bubbles, online status indicators, and real-time updates.
    *   **NetworkManager:** Acts as the bridge between the GUI and the socket. It runs a background receiver thread to listen for incoming messages without blocking the Swing Event Dispatch Thread (EDT).
    *   **ReconnectionHandler:** A background daemon thread that automatically attempts to restore dropped connections seamlessly (up to 10 attempts).
*   **Protocol (`common` package):**
    *   A custom text-based protocol using pipe (`|`) delimiters.
    *   Message formats include: `USERNAME|name`, `CHAT|sender|text|HH:mm`, `SYSTEM|text|HH:mm`, and `USERLIST|user1,user2`.

## 🛠️ Technologies Used

This project strictly adheres to standard Java SE, utilizing only built-in libraries with no external dependencies (No Maven/Gradle).

*   **Networking:** `java.net.Socket` and `java.net.ServerSocket` for reliable TCP communication (No UDP).
*   **I/O Streams:** `java.io.BufferedReader` and `java.io.PrintWriter` for efficient data transmission over network streams.
*   **Multithreading:** `java.lang.Thread` and `java.lang.Runnable` for handling concurrent clients and offloading network operations from the main UI thread.
*   **Concurrency:** `java.util.concurrent.ConcurrentHashMap` and `synchronized` blocks to ensure data integrity in shared state environments.
*   **GUI Toolkit:** `javax.swing.*` and `java.awt.*` for rendering the graphical user interface.

## 🚀 How to Run

### Option 1: Using the Batch Scripts (Windows - Recommended)
For a quick and easy start, use the included automation scripts. They automatically compile the source code and start the application.

1.  Double-click `run_server.bat` to start the server (runs on port 8080 by default).
2.  Double-click `run_client.bat` to launch the GUI Client.
3.  *(Optional)* You can run `run_client.bat` multiple times to simulate multiple different users connecting to the same server.

### Option 2: Manual Execution via Terminal
If you prefer running the application manually from the command line, ensure your terminal is opened in the **root directory** of the project.

**1. Compile the code:**
```bash
javac common/*.java server/*.java client/*.java ui/*.java
```

**2. Start the Server:**
*(Syntax: java server.Server <port>)*
```bash
java server.Server 8080
```

**3. Start the Client GUI:**
*(Open a new terminal window for this)*
```bash
java ui.ChatGUI
```

## 📁 Project Structure

```text
├── client/                 # Client-side network logic
│   ├── Client.java         # Console fallback client
│   ├── ConnectionConfig.java
│   ├── NetworkManager.java
│   └── ReconnectionHandler.java
├── common/                 # Shared utilities
│   ├── Protocol.java
│   └── TimeUtil.java
├── server/                 # Server-side logic
│   ├── ClientHandler.java
│   ├── Server.java
│   └── UsersRegistry.java
├── ui/                     # Swing GUI components
│   ├── ChatGUI.java
│   ├── LoginDialog.java
│   ├── MessageBubble.java
│   └── UserListRenderer.java
├── run_client.bat          # Automation script
└── run_server.bat          # Automation script
```
