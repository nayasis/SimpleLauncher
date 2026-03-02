# Simple Launcher

## Introduction

**Simple Launcher** helps you launch applications quickly and find them easily.
You can configure each launcher item with a **relative path** or a **custom command**.

![screenshot](https://github.com/nayasis/simpleLauncher/blob/master/doc/screenshot.jpg?raw=true)

## Requirements

- Java 17 or higher

## Build

#### 1. Clone source

```shell
git clone https://github.com/nayasis/SimpleLauncher.git
```

#### 2. Build

```shell
gradlew build
```

#### 3. Run

```shell
gradlew run
```

#### 4. Create Native Executable (clean build)

- Create a native executable (`.exe` on Windows) with jpackage:

```shell
gradlew clean createNativeExe
```

- The executable is generated in `build/dist/simplelauncher/`.

#### 5. Create Native Executable

- Create a native executable (`.exe` on Windows) with jpackage:

```shell
gradlew createNativeExe
```

- The executable is generated in `build/dist/simplelauncher/`.

## Migration

In most cases, upgrading to a newer version works without manual migration.

If your catalog or settings are not preserved after an upgrade, use file export/import:

1. In the previous version, export the catalog (`Ctrl + Shift + X`).
2. Upgrade or install the new version.
3. In the new version, import the catalog (`Ctrl + Shift + I`).

This is the recommended fallback method when automatic upgrade does not work as expected.

## Shortcuts

### Menu

| Shortcut             | Description                 |
|----------------------|-----------------------------|
| Ctrl + Shift + **I** | Import application catalog  |
| Ctrl + Shift + **X** | Export application catalog  |
| Ctrl + Shift + **D** | Clear application catalog   |
| Alt + **E**          | Toggle detail launcher editor |
| Alt + **V**          | Toggle menu bar             |
| Alt + **G**          | Toggle group filter         |
| Ctrl + Shift + **F** | Keep window always on top   |
| **F1**               | Show help                   |

### Main Catalog

| Shortcut     | Description              |
|--------------|--------------------------|
| Enter        | Execute item             |
| Delete       | Delete item              |
| Ctrl + **C** | Copy item folder path    |

### Link Editor

Only works when the Link Editor is open.

| Shortcut             | Description              |
|----------------------|--------------------------|
| Ctrl + **N**         | New item                 |
| Ctrl + Shift + **N** | Create link from file    |
| Shift + **Del**      | Delete item              |
| Ctrl + **D**         | Duplicate item           |
| Ctrl + **S**         | Save item                |
| Ctrl + Shift + **C** | Copy item folder path    |
| Ctrl + **O**         | Open item folder         |
| Ctrl + **I**         | Change item icon         |

![link editor](https://github.com/nayasis/simpleLauncher/blob/master/doc/link-editor.jpg?raw=true)

### Keyword History

When the keyword field is focused:

![keyword focused](https://github.com/nayasis/simpleLauncher/blob/master/doc/keyword-focused.jpg?raw=true)

Enter history mode by pressing **Alt + Down**.

![history mode](https://github.com/nayasis/simpleLauncher/blob/master/doc/keyword-historymode.jpg?raw=true)

#### Keyword History Functions

| Shortcut      | Description |
|---------------|-------------|
| Alt + **Left**  | Move to the previously executed link |
| Alt + **Right** | Move to the next executed link |
| Any key         | Filter executed links ![autocompleted links](https://github.com/nayasis/simpleLauncher/blob/master/doc/keyword-autocomplete.jpg?raw=true) |
| Esc             | Exit history mode |

## Binding Parameters

An item's **option** (or prefix option) can include placeholders.
When you drag and drop a file (or directory) onto an item, these placeholders are replaced with path values.

| Parameter      | Description                 | Example                |
|----------------|-----------------------------|------------------------|
| \${path}       | Full file path              | \\usr\\path\\readme.md |
| \${path-unix}  | Full file path (Unix style) | /usr/path/readme.md    |
| \${dir}        | File directory path         | \\user\\path           |
| \${dir-unix}   | File directory (Unix style) | /user/path             |
| \${file}       | File name                   | readme.md              |
| \${name}       | File name without extension | readme                 |
| \${ext}        | Extension                   | md                     |
| \${home}       | User home directory         | \\home\\***user***     |
| \${home-unix}  | User home directory (Unix style) | /home/***user*** |

**Note:** `\${path}` and `\${dir}` are different only on Windows.

### Example

```shell
ape2wav.exe "${path}" "${dir}\\${name}.wav"
```

## Contact

- Issues: https://github.com/nayasis/SimpleLauncher/issues
- Email: [nayasis@gmail.com](mailto:nayasis@gmail.com)
