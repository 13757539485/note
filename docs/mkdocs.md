## 环境要求
[安装python](python.md)

## mkdocs官网

https://www.mkdocs.org/user-guide/configuration/#build-directories

## 安装mkdocs
```
pip install mkdocs
```

## 创建项目
```
mkdocs new xxx
```

## 查看预览

```
cd xxx
mkdocs serve
```
在http://127.0.0.1:8000进行查看

## 生成网页

```
mkdocs build
```

## 安装material主题
```
pip install mkdocs-material
```

## 配置主题
```
theme: 
  name: material
  language: zh
  custom_dir: docs/custom_theme
  palette:
    primary: 'Light Blue'
    accent: 'Light Blue'
  features:
    - navigation.tabs
  font:
    text: Microsoft YaHei
    code: Courier New
```

## 其他配置
```
markdown_extensions:
  - admonition
  - codehilite:
      guess_lang: false
      linenums: false
  - toc:
      permalink: true
  - footnotes
  - meta
  - def_list
  - pymdownx.arithmatex
  - pymdownx.betterem:
      smart_enable: all
  - pymdownx.caret
  - pymdownx.critic
  - pymdownx.details
  - pymdownx.emoji:
      emoji_generator: !!python/name:pymdownx.emoji.to_png
      #emoji_generator: !!python/name:pymdownx.emoji.to_svg
      #emoji_generator: !!python/name:pymdownx.emoji.to_png_sprite
      #emoji_generator: !!python/name:pymdownx.emoji.to_svg_sprite
      #emoji_generator: !!python/name:pymdownx.emoji.to_awesome
      #emoji_generator: !!python/name:pymdownx.emoji.to_alt
  - pymdownx.inlinehilite
  - pymdownx.magiclink
  - pymdownx.mark
  - pymdownx.smartsymbols
  - pymdownx.superfences
  - pymdownx.tasklist
  - pymdownx.tilde

extra:
  search:
    language: 'en,zh,jp'

plugins:
  - search
```

## 发布

1. github创建项目并clone到mkdocs目录
2. 执行一下命令，工具就会自动将相应内容推送到项目的 gh-pages 分支上
```
mkdocs gh-deploy
```
3. 浏览https://username.github.io/project-name

## 解决中文搜索问题

https://segmentfault.com/a/1190000018592279

- 其中文件路径在win上不对，直接使用vscode打开python中的安装文件夹(Lib\site-packages\mkdocs)，搜索方法名即可
- 修改时需要安装jieba，手动import jieba
```
pip install jieba
```