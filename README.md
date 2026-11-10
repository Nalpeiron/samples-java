# Zentitle Java sample

Run this console sample with the precompiled Java Licensing Client SDK JAR, its matching Maven POM, and the Zentitle2Core native libraries.

## 1. Prerequisites

JDK 17 or higher and Maven 3.9.x or higher. Check the selected SDK's release notes for any higher JDK requirement.

## 2. Download the sample and SDK

Clone the [Java sample repository](https://github.com/Nalpeiron/samples-java):

```sh
git clone https://github.com/Nalpeiron/samples-java.git
```

Download the latest stable SDK from the [Java Licensing Client release notes](https://docs.nalpeiron.com/zentitle2-docs/developers/the-licensing-client-java/release-notes) and use sample sources compatible with that release. Package registry credentials are available in Zentitle2 under **Administration > API Credentials > Package Registry**.

Replace `SDK_VERSION` throughout these instructions with the downloaded version. Extract the SDK and copy these files into an `sdk` directory next to `samples-java`:

- `licensing-client-SDK_VERSION.jar`, typically under `licensing-client/target/`.
- The SDK module's `licensing-client/pom.xml`, renamed to `licensing-client-SDK_VERSION.pom`.

Use the original module POM from the same release so Maven resolves the SDK's transitive dependencies. Its artifact ID must be `licensing-client`.

## 3. Install the SDK JAR

From the `sdk` directory:

```sh
mvn install:install-file "-Dfile=licensing-client-SDK_VERSION.jar" "-DpomFile=licensing-client-SDK_VERSION.pom"
```

In `samples-java/pom.xml`, set the existing `com.nalpeiron.zentitle:licensing-client` dependency version to `SDK_VERSION`.

## 4. Prepare the native libraries

Download a compatible Zentitle2Core binary package from the [Core release notes](https://docs.nalpeiron.com/zentitle2-docs/developers/the-licensing-client-c++/release-notes). Java SDK and Core releases have separate version numbers.

Under `language_bindings/java`, select the Java bindings for your operating system and JVM architecture. Keep both libraries from the same compatible Core release and copy them into the `samples-java` root, alongside `pom.xml`:

| Platform | JNI library | Core library |
| --- | --- | --- |
| macOS | `libZentitleJNI.dylib` | `libZentitle2Core.dylib` |
| Linux | `libZentitleJNI.so` | `libZentitle2Core.so` |
| Windows | `ZentitleJNI.dll` | `Zentitle2Core.dll` |

Maven does not download, copy, or package these native files. See the [Java Core integration guide](https://docs.nalpeiron.com/zentitle2-docs/developers/the-licensing-client-java/zentitle2core-library) for further details.

## 5. Configure your tenant and product

Create `appsettings.json` in the `samples-java` root. Obtain the values from your Zentitle2 administrator or the tenant's API credentials and product settings.

```json
{
  "UseCoreLibrary": true,
  "PrintLibraryLogs": true,
  "Licensing": {
    "ApiUrl": "<YOUR_LICENSING_API_BASE_URL>",
    "TenantId": "<YOUR_TENANT_ID>",
    "TenantRsaKeyModulus": "<YOUR_TENANT_PUBLIC_RSA_KEY_MODULUS>",
    "ProductId": "<YOUR_PRODUCT_ID>"
  }
}
```

| Setting | Value |
| --- | --- |
| `ApiUrl` | Licensing API base URL, without an operation path. Use the Licensing API endpoint rather than the Management API endpoint. |
| `TenantRsaKeyModulus` | Public RSA modulus for offline activation, not a PEM certificate or private key.   |

The sample reads `appsettings.json` from its working directory, falling back to the source-tree template if absent. Keep the root configuration file with your runtime files.

`UseCoreLibrary=true` enables fingerprinting, encrypted storage, and offline activation. Setting it to `false` selects manual seat IDs and plaintext storage at `<user.home>/Z2_OnlineActivation_Console/license.json`, and disables offline operations. Native libraries are still required because this sample's `SharedActivation` system lock uses JNI.

`PrintLibraryLogs=true` writes native diagnostics to `core-lib-test.log` in the working directory.

## 6. Build and run

Build from `samples-java`:

```sh
mvn clean package
```

Replace `SAMPLE_VERSION` below with the sample project version from `pom.xml`, or use the exact JAR filename printed by Maven. `SAMPLE_VERSION` version is independent of `SDK_VERSION`.

Run from the same directory on macOS or Linux:

```sh
java -Djava.library.path=. -jar target/samples-SAMPLE_VERSION-jar-with-dependencies.jar
```

On Windows PowerShell:

```powershell
$env:PATH = "$($PWD.Path);$env:PATH"
java "-Djava.library.path=." -jar target/samples-SAMPLE_VERSION-jar-with-dependencies.jar
```

For an IDE launch, use main class `com.nalpeiron.zentitle.sample.Main`, the `samples-java` root as the working directory, and JVM option `-Djava.library.path=.`.

## 7. Activate a license

Use a valid activation code for the configured tenant and product, with an available entitlement seat.

1. Select **Activate license with code** and enter the code and optional seat name.
2. Choose device fingerprinting for the seat ID or enter a stable seat ID manually.
3. Confirm that the displayed activation state is `ACTIVE`.

On later runs, answer no to deleting saved activation data to resume it. Deleting local data does not deactivate the server-side seat.

Core-enabled storage is `Z2_OnlineActivation_Console/license.encrypted` under the operating system's per-user data directory. Startup logs show the resolved path.

For [offline activation](https://docs.nalpeiron.com/zentitle2-docs/developers/offline-activation), enable Core, configure the tenant's public RSA modulus, and set an **Offline Lease Period** on the product or entitlement. Follow the linked portal workflow using the sample's offline menu operations.

## Troubleshooting

| Symptom | What to check |
| --- | --- |
| SDK dependency cannot be resolved | Install the matching JAR and module POM, and check the dependency version and Maven repository settings. |
| Missing Java classes at runtime | Install the SDK with its original module POM, rebuild, and run the `jar-with-dependencies` JAR. |
| Native loading or method lookup fails | Check the working directory, `java.library.path`, OS, JVM architecture, required native dependencies, and compatibility between the SDK and both native files. |

On Linux, if a dependent native library cannot be found, add the working directory to the loader path before rerunning the launch command:

```sh
export LD_LIBRARY_PATH="$PWD${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
```
