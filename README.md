# Java Ollama Telegram Bot

This is a simple yet powerful Telegram bot written in Java that connects to a local or remote [Ollama](https://ollama.com/) instance to provide conversational AI capabilities. It's designed to be easily configurable and deployable using Docker.

## Features

*   **Conversational AI:** Integrates with any model served by Ollama (e.g., Llama 3, Mistral).
*   **Persistent Chat History:** Remembers conversation history for each user/chat separately, saving it to a JSON file.
*   **Dockerized:** Comes with a simple shell script (`makeDocker.sh`) to build and run the bot in a Docker container.
*   **Highly Configurable:** Easily change the bot token, Ollama endpoint, model, system prompt, and other parameters via a JSON config file.
*   **"Thinking Process" Display:** A unique feature that can parse `<think>...</think>` tags from the model's output and display them as a collapsible spoiler in Telegram, offering insights into the AI's reasoning.

## Prerequisites

Before you begin, ensure you have the following installed:

*   Docker
*   A running Ollama instance, accessible from where you run the bot.
*   A Telegram Bot Token. You can get one from the BotFather.

## Configuration

The bot is configured using a JSON file. The `makeDocker.sh` script expects this file to be named `config.json` by default. 
Change this setting in the `makeDocker.sh`

1.  Create a file named `config.json` in the root of the project directory. You can copy `config.example.json` to get started.
2.  Fill in the details for your setup.

#### Example `config.json`:

```json
{
  "botToken": "123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11",
  "ollamaUrl": "http://192.168.1.100:11434",
  "ollamaModel": "gemma3",
  "systemPrompt": "You are Ada Lovelace, famous mathematician and computer scientist. Only speak in the style of a 19th century Oxford scholar. You are concise in your answers.",
  "maxHistoryMessages": 10,
  "numCtx": 4096,
  "temperature": 0.7
}
```

#### Configuration Options:

| Key                  | Description                                                                                             |
| -------------------- | ------------------------------------------------------------------------------------------------------- |
| `botToken`           | Your unique token from Telegram's BotFather.                                                            |
| `ollamaUrl`          | The base URL for your Ollama API endpoint. **Do not** include `/api/chat`.                              |
| `ollamaModel`        | The name of the model you want to use (e.g., `llama3`, `mistral`).                                      |
| `systemPrompt`       | The system prompt to guide the model's behavior and personality.                                        |
| `maxHistoryMessages` | The maximum number of previous messages (user and assistant) to send to the model as context.           |
| `numCtx`             | The context window size for the model. Controls how many tokens the model can "remember".               |
| `temperature`        | The model's temperature. Higher values (e.g., 1.0) make output more random, lower values (e.g., 0.2) make it more deterministic. |


## How to Run (Docker)

The recommended way to run the bot is using the provided Docker script.

1.  **Configure the bot:** Create your `myBot.json` configuration file as described above.

2.  **Update the dockerfile to point to your config file:**

3.  **build the uberjar(shadowJar):** ./gradlew shadowJar

4. **edit the makeDocker.sh script to point to your config file `myBot.json`:**
    ```bash
    vi makeDocker.sh
    ```

5.  **Make the script executable:**
    ```bash
    chmod +x makeDocker.sh
    ```

6.  **Run the script:**
    ```bash
    ./makeDocker.sh
    ```

This script will:
*   Stop and remove any previous container named `MyTgBot`.
*   Create a `./data` directory on your host if it doesn't exist.
*   Build the Docker image for the Java application.
*   Run a new container in the background.
*   Mount your `myBot.json` file (read-only) into the container.
*   Mount the `./data` directory into the container to persist `histories.json` across restarts.
*   Histories will be lost if the docker is rebuilt

To see the bot's logs, you can run:
```bash
docker logs -f MyTgBot
```

## Special Feature: The `<think>` Tag

You can instruct your model to wrap its reasoning, planning, or internal monologue in `<think>` tags. The bot will automatically extract this content, hide it in a "Thinking Process" spoiler, and present the main answer cleanly.


## Local Development (Without Docker)

If you wish to run the bot directly on your machine for development purposes:

1.  Ensure you have a recent JDK (Java Development Kit) and Gradle installed.
2.  Create your configuration file (e.g., `bot2.json`).
3.  Run the application using the Gradle wrapper, passing the path to your config file as an argument:
    ```bash
    ./gradlew run --args="bot2.json"
    ```

