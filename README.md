# Simple Launcher

## Introduction

***Simple Launcher*** is designed to launch all of your application simply and to search easily.

It accepts executable command or relative path to run so you could customize your own application launcher independent with OS.

![screenshot](https://github.com/nayasis/SimpleLauncher/blob/master/doc/screenshot.jpg?raw=true)

## Requirements

- Java 12 above
- JavaFX 12 above

## CommandLine Arguments

| argument       | description                                         |
| -------------- | --------------------------------------------------- |
| help (or h)    | show help                                           |
| clear          | clear memorized application configuration           |
|                | (for example, last window position)                 |

### example

    java -jar SimpleLauncher.jar clear


## Shortcuts

### Menu

| shortcut             | description                   |
| --------------       | ------------------            |
| Ctrl + Shift + **I** | import application catalog    |
| Ctrl + Shift + **X** | export application catalog    |
| Ctrl + Shift + **D** | clear  application catalog    |
| ALT + **E**          | toggle detail launcher editor |
| ALT + **V**          | toggle menu bar               |
| Ctrl + Shift + **F** | set windows on top always     |
| **F1**               | show help                     |

### Main catalog

| shortcut       | description        |
| -------------- | ------------------ |
| Enter          | execute item       |
| Delete         | delete item        |

### Launcher editor

only works when Launcher editor is opened.

| shortcut       | description             |
| -------------- | ------------------      |
| Ctrl + **N**   | new item                |
| Ctrl + **D**   | delete item             |
| Ctrl + **C**   | copy item               |
| Ctrl + **S**   | save item               |
| Ctrl + **F**   | copy item's folder path |
| Ctrl + **O**   | open item's folder      |
| Ctrl + **I**   | change item icon        |


## Binding parameters

Item's ***option***(or prefix option) could have parameter and it would replace to file(or directory) path
being injected when runs by file(or directory) dragging.

| parameter      | description                 | example                |
|----------------|-----------------------------|------------------------|
| \#{path}       | file's full path            | \\usr\\path\\readme.md |
| \#{path-unix}  | file's full path as unix    | /usr/path/readme.md    |
| \#{dir}        | file's directory            | \\user\\path           |
| \#{dir-unix}   | file's directory as unix    | /user/path             |
| \#{file}       | file name                   | readme.md              |
| \#{name}       | file name without extension | readme                 |
| \#{ext}        | extension                   | md                     |
| \#{home}       | user home directory         | \\home\\***user***     |
| \#{home-unix}  | user home directory as unix | /home/***user***       |

### Example

ape2wav.exe "#{filepath}" "#{dir}\\#{name}.wav"

## Contact

FAQs  : https://github.com/nayasis/simpleLauncher2/issues

email : [nayasis@gmail.com](mailto:nayasis@gmail.com)
