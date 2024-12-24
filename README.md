# OBD Logger

OBD Logger is an Android application that connects to a Bluetooth OBD-II device to retrieve and display various engine parameters. 
This application is created solely for testing purposes to demonstrate how to use the `https://github.com/eltonvs/kotlin-obd-api` library.

## Features

- Connect to a Bluetooth OBD-II device
- Retrieve and display engine parameters such as speed, RPM, mass air flow, runtime, load, and throttle position
- Display status messages and error messages

## Requirements

- Android device with Bluetooth support
- Bluetooth OBD-II device (e.g., V-LINK)

## Permissions

The application requires the following permissions:

```xml
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
```

## Installation

1. Clone the repository:
    ```sh
    git clone https://github.com/yourusername/obd-logger.git
    ```
2. Open the project in Android Studio.
3. Build and run the application on your Android device.

## Usage

1. Ensure that your Bluetooth OBD-II device is paired with your Android device.
2. Open the OBD Logger application.
3. Click the "Connect" button to connect to the OBD-II device.
4. The application will display the retrieved engine parameters and status messages.

## Code Overview

### MainActivity

The `MainActivity` class is the entry point of the application. It initializes the Bluetooth adapter and sets up the UI.

### ConnectThread

The `ConnectThread` class handles the Bluetooth connection to the OBD-II device and retrieves engine parameters.

### Utils

The `Utils` class contains utility functions, such as `rememberStatusBarHeight`, to handle UI-related tasks.

### MainScreen

The `MainScreen` composable function defines the main UI of the application, including the connect button and status messages.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
```
