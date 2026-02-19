# DeathSwap

**DeathSwap** is a popular minecraft minigame where you try to trap the other players, every time a swap happens, and you try to survive what the other has set for you!

## 🚀 Features
- **World Management**: Separate game world for each game, which makes every world different.
- **State Restoration**: Players get restored back to the normal world after the game ends.
- **Late comers**: Players who join late can still spectate and players who disconnect have 3 minutes to rejoin.

## 🛠️ Installation

1. Download the latest `DeathSwap.jar`.
2. Place the JAR file into your server's `plugins` folder.
3. Restart the server
4. (Optional) Configure settings in `plugins/DeathSwap/config.yml`.

## 🎮 Commands & Permissions

### Commands
| Command | Description                                |
| :--- |:-------------------------------------------|
| `/deathswap start` | Starts a new game with all online players. |
| `/deathswap stop` | Forcefully ends the current game.          |
| `/deathswap reload` | Reloads the plugin configuration.          |
| `/deathswap spectate` | Specate the curerent game session.         |
| `/deathswap leave` | Leave the current game session.            |

### Permissions
- `deathswap.use`: Allows players to use basic commands (spectate, leave). (Default: true)
- `deathswap.admin`: Allows administrators to start, stop, and reload the game. (Default: OP)

## ⚙️ Configuration

The `config.yml` file allows you to customize the game experience:

```yaml
game:
  # Total duration of the match in seconds
  total-time-seconds: 600
  
  # Interval between player swaps in seconds
  cooldown-seconds: 60

  # Radius for random spawns in the game world
  radius: 100
```

### Requirements
- **Java 21**
- **Gradle**

### Building
To build the plugin from source, run:
```bash
./gradlew build
```
The compiled JAR will be located in `build/libs/`.

## 📜 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
