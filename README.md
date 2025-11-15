# Simple Launcher

## Introduction

***Simple Launcher*** is designed for launching all of your application simply and searching easily.  
You could use **relative path** or **configurable command** in your own application launch freely.

![screenshot](https://github.com/nayasis/simpleLauncher/blob/master/doc/screenshot.jpg?raw=true)

## Requirements
- Java 17 above

## Build
#### 1. Download source
```shell
git clone https://github.com/nayasis/SimpleLauncher.git
```

#### 2. Build
```shell
gradlew build
```

#### 3. Execution
```shell
gradlew run
```

#### 4. Create Native Executable
- Create a native executable (.exe for Windows) using jpackage:
```shell
gradlew clean createNativeExe
```
- The executable will be created in `build/dist/simplelauncher/` directory.

#### 5. Create Native Executable with jpackage
- Create a native executable (.exe for Windows) using jpackage:
```shell
gradlew createNativeExe
```
- The executable will be created in `build/dist/simplelauncher/` directory.
- Note: Requires Java 14 or higher with jpackage tool (included in JDK 14+).

#### 5. Create Native Executable with jpackage
- Create a native executable (.exe for Windows) using jpackage:
```shell
gradlew createNativeExe
```
- The executable will be created in `build/dist/simplelauncher/` directory.
- Note: Requires Java 14 or higher with jpackage tool (included in JDK 14+).

## Shortcuts

### Menu

| shortcut             | description                   |
|----------------------|-------------------------------|
| Ctrl + Shift + **I** | import application catalog    |
| Ctrl + Shift + **X** | export application catalog    |
| Ctrl + Shift + **D** | clear  application catalog    |
| ALT + **E**          | toggle detail launcher editor |
| ALT + **V**          | toggle menu bar               |
| ALT + **G**          | toggle group filter           |
| Ctrl + Shift + **F** | set windows on top always     |
| **F1**               | show help                     |

### Main catalog

| shortcut     | description             |
|--------------|-------------------------|
| Enter        | execute item            |
| Delete       | delete item             |
| Ctrl + **C** | copy item's folder path |

### Link editor

Only works when Link editor is opened.

| shortcut             | description             |
|----------------------|-------------------------|
| Ctrl + **N**         | new item                |
| Ctrl + Shift + **N** | create link via file    |
| Shift + **DEL**      | delete item             |
| Ctrl + **D**         | duplicate item          |
| Ctrl + **S**         | save item               |
| Ctrl + Shift + **C** | copy item's folder path |
| Ctrl + **O**         | open item's folder      |
| Ctrl + **I**         | change item icon        |

![link editor](https://github.com/nayasis/simpleLauncher/blob/master/doc/link-editor.jpg?raw=true)

### Keyword history

When keyword focused

![keyword focused](https://github.com/nayasis/simpleLauncher/blob/master/doc/keyword-focused.jpg?raw=true)

Enter history mode by pressing ***Alt + Down***.

![history mode](https://github.com/nayasis/simpleLauncher/blob/master/doc/keyword-historymode.jpg?raw=true)

#### keyword history functions

| shortcut        | description                                                                                                                               |
|-----------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| Alt + **Left**  | previous link executed                                                                                                                    |
| Alt + **Right** | next link executed                                                                                                                        |
| any key         | filter executed links ![autocompleted links](https://github.com/nayasis/simpleLauncher/blob/master/doc/keyword-autocomplete.jpg?raw=true) |
| Esc             | exit history mode                                                                                                                         |

## Binding parameters

Item's ***option***(or prefix option) could have parameter and it would replace to file(or directory) path
being injected when runs by file(or directory) dragging.

| parameter      | description                 | example                |
|----------------|-----------------------------|------------------------|
| \${path}       | file's full path            | \\usr\\path\\readme.md |
| \${path-unix}  | file's full path as unix    | /usr/path/readme.md    |
| \${dir}        | file's directory            | \\user\\path           |
| \${dir-unix}   | file's directory as unix    | /user/path             |
| \${file}       | file name                   | readme.md              |
| \${name}       | file name without extension | readme                 |
| \${ext}        | extension                   | md                     |
| \${home}       | user home directory         | \\home\\***user***     |
| \${home-unix}  | user home directory as unix | /home/***user***       |

** note: \${path} and \${dir} are only different in Windows.

### Example

ape2wav.exe "${path}" "${dir}\\${name}.wav"


## Contact

- issue : https://github.com/nayasis/SimpleLauncher/issues
- email : [nayasis@gmail.com](mailto:nayasis@gmail.com)
