# JSnip 

[![CodeQL](https://github.com/CHeuberger/JSnip/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/CHeuberger/JSnip/actions/workflows/codeql-analysis.yml)

A~nother~ screenshot program.

This was developed based on the following use cases:

- take and show multiple screenshots:
  Documents often have more than one reference to tables and pictures on different pages. This program allows taking screenshots of these and move them around as needed, without using any additional screen space like for title or toolbars.
- take multiple screenshots of the same region:
  This allows to recapture a screenshot of the same screen region to help creating documentations. IMO it looks nicer if screenshots of the (same) GUI are of the same size and location.
- fun: just wanted to develop it.

It uses the `java.awt.Robot` class to generate to take the screenshot.

### Usage

Java Runtime Environment (JRE) or Java Development Kit (JDK) for Java 8 must be installed. Download the JAR file and start it (double click or `java -jar jsnip.jar`).

An icon (<img src="src/resources/cfh/jsnip/tray.png" alt="tray" width="12" height="12" />) will be added to notification area of the taskbar. 

- `Left-Click` to take a screenshot
- `Right-Click` for menu (including the Help)

### Disclaimer

BECAUSE THE PROGRAM IS FREE OF CHARGE, THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTEND PERMITTED BY LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR OR CORRECTION.
