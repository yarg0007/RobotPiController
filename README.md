# RobotPiController

Android controller app for a Raspberry Pi robot. Connects over SSH and HTTP to [RobotPiServer](https://github.com/yarg0007/RobotPiServer), streams live video from the Pi camera, streams bidirectional audio, and sends robot control commands via dual joypads.

Example robot: BruceBot1000 — https://www.youtube.com/watch?v=05eA5SQ0DeI

---

## Prerequisites

- **Android Studio** (Hedgehog or later recommended) — for building
- **Android device** running API 21+ (Android 5.0+) in landscape orientation
- **Raspberry Pi** running [RobotPiServer](https://github.com/yarg0007/RobotPiServer) on the same network as the Android device
- **ADB** — for sideloading during development

---

## Building

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Install to a connected device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Configuration

Tap the **gear icon** (top-left) to open the Config screen. All settings are saved on-device.

| Setting | Description | Default |
|---|---|---|
| SSH Host | Pi IP address — **use the IP, not hostname** (e.g. `192.168.1.42`). mDNS (`raspi.local`) may not resolve from Android. Run `hostname -I` on the Pi to find the address. | — |
| SSH Port | SSH port on the Pi | `22` |
| SSH Username | Pi SSH username | — |
| SSH Password | Pi SSH password | — |
| Robot Audio Port | UDP port for audio streaming. Must match `audioStreamServer.receivePort` in the server's `config.json`. | `49809` |
| Server HTTP Port | Port for the HTTP `/connect` and `/disconnect` API. Must match `serverPort` in the server's `config.json`. | `8001` |
| Video Stream Port | RTSP port for the live camera feed. Must match `videoStreamPort` in the server's `config.json`. | `8554` |

---

## Connection Flow

Tapping **CONNECT**:

1. SSH to the Pi → kills any existing server instance → starts `RobotPiServer` JAR in the background
2. HTTP `GET /connect` → server starts video/audio/control streams
3. Live camera feed appears in the app background via RTSP (`MediaPlayer`)
4. Dual joypads become active for robot control

Tapping **DISCONNECT** shows a dialog:
- **Yes** — stops all streams and shuts down the Pi
- **No** — stops all streams, server stays running (Pi remains on)

---

## Permissions

| Permission | When requested |
|---|---|
| `RECORD_AUDIO` | Requested at runtime on first launch (required for microphone audio streaming) |
| `INTERNET` | Declared in manifest (required for SSH, HTTP, and audio/video UDP sockets) |

---

## Running Tests

Unit tests run on the JVM (no device required):

```bash
./gradlew test
```

Test coverage includes:
- `SshManagerTest` / `SshCommandPayloadTest` — SSH command lifecycle
- `ServerConnectionClientTest` — HTTP `/connect` and `/disconnect` against a real embedded server
- `ServerConnectionThreadTest` — retry logic and observer callbacks with mocked HTTP client

---

## Architecture

```
MainActivity
├── SshManager          — opens SSH, starts RobotPiServer JAR on Pi
├── ServerConnectionThread — calls /connect or /disconnect via HTTP
├── VideoStream (SurfaceView) — receives RTSP feed via MediaPlayer
├── AudioStreamClient   — bidirectional UDP audio
└── ControllerInputThread — sends gamepad state to Pi via UDP
```

The video stream displays full-screen behind the UI controls. The RTSP stream URL is `rtsp://<pi-ip>:<videoStreamPort>/` — the Pi serves it via VLC (`cvlc`) piped from `raspivid`.

---

## Troubleshooting

**"Error creating SSH connection"** — check that SSH Host is the Pi's IP address, not `raspi.local`. Run `hostname -I` on the Pi.

**Video not appearing** — ensure the Pi camera is enabled and VLC is installed on the Pi. The stream starts ~15 seconds after CONNECT is tapped (server startup + RTSP negotiation). If video never appears, SSH to the Pi and check `/tmp/robotpi.log`.

**RECORD_AUDIO permission denied** — the runtime permission dialog appears on first launch. If it was dismissed, grant it manually: Settings → Apps → RobotPiController → Permissions → Microphone.

**App shows "Failed to connect to robot server"** — the server retries up to 5 times with 3-second delays. If it still fails, check that `serverHttpPort` in the app matches `serverPort` in the Pi's `config.json`, and that the server started correctly (`cat /tmp/robotpi.log` on the Pi).
