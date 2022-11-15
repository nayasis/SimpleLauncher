# Simple Launcher

## Introduction

***Simple Launcher*** is designed to launch all of your application simply and to search easily.

You could use configurable command (include relative path) so customize your own application launcher regardless of any OS.

![screenshot](https://github.com/nayasis/simpleLauncher/blob/master/doc/screenshot.jpg?raw=true)

## Requirements

- Java 11 above
- JavaFX 13 above

## Execution
```shell
java -jar SimpleLauncher.jar clear
```

### For Windows user

There is executable wrapper for window user.
- [32bit executable wrapper](https://github.com/nayasis/SimpleLauncher/blob/master/work/wrapper/WinRun4J/32/wrapper.zip?raw=true)
- [64bit executable wrapper](https://github.com/nayasis/SimpleLauncher/blob/master/work/wrapper/WinRun4J/64/wrapper.zip?raw=true)

Extract executable wrapper on same directory with ***simplelauncher.jar***
```shell
simplelauncher.exe
simplelauncher.ini
simplelauncher.jar
```
Run ***simplelauncher.exe***


## Shortcuts

### Menu

| shortcut             | description                   |
| --------------       | ------------------            |
| Ctrl + Shift + **I** | import application catalog    |
| Ctrl + Shift + **X** | export application catalog    |
| Ctrl + Shift + **D** | clear  application catalog    |
| ALT + **E**          | toggle detail launcher editor |
| ALT + **V**          | toggle menu bar               |
| ALT + **G**          | toggle group filter           |
| Ctrl + Shift + **F** | set windows on top always     |
| **F1**               | show help                     |

### Main catalog

| shortcut       | description             |
| -------------- | ------------------      |
| Enter          | execute item            |
| Delete         | delete item             |
| Ctrl + **C**   | copy item's folder path |

### Link editor

Only works when Link editor is opened.

| shortcut             | description             |
| --------------       | ------------------      |
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

| shortcut         | description             |
| --------------   | ------------------      |
| Alt + **Left**   | previous link executed  |
| Alt + **Right**  | next link executed      |
| any key          | filter executed links ![autocompleted links](https://github.com/nayasis/simpleLauncher/blob/master/doc/keyword-autocomplete.jpg?raw=true)  |

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

### Example

ape2wav.exe "${path}" "${dir}\\${name}.wav"

## Development

### Gradle

This is **SpringBoot** application and run through **Spring Boot Loader**. 

#### Run from source
```shell
gradle bootRun
```

#### Build
```shell
gradle bootJar
```
JAR file will be created in <u>${Project Directory}**/build/libs**</u>  
It could be run like below.
```shell
java -jar simplelauncher.jar
```


## Contact

- issue : https://github.com/nayasis/SimpleLauncher/issues
- email : [nayasis@gmail.com](mailto:nayasis@gmail.com)
