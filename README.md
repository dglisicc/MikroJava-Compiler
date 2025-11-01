MikroJava-Compiler

A MikroJava compiler with full support for lexical, syntax, and semantic analysis, bytecode generation, and execution on a MikroJava virtual machine. This project was developed for educational purposes during the Programming Translators 1 course.

🚀 Project Overview

This compiler translates source code written in MikroJava into bytecode executable on the MikroJava Virtual Machine (MJVM). It supports all major compilation stages:

Lexical Analysis (JFlex)

Syntax Analysis (CUP)

Semantic Analysis

Code Generation

Bytecode Execution

The goal of the project is to demonstrate the full compilation process, from parsing and building the abstract syntax tree (AST) to generating executable code.

🧩 Project Structure

├── src/            # Source code (lexer, parser, AST, semantic checks, codegen, runtime)

├── test/           # Sample MikroJava programs

├── spec/           # Grammar and specification files (JFlex, CUP)

├── build.xml       # Build configuration for compilation and execution

└── README.md       # Project documentation
