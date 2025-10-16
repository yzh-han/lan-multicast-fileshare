# Distributed File Sharing System

A LAN-based, distributed file-share application using IPv6 multicast for discovery and control messaging, with TCP/IPv6 for file transfers. 

This is the practical assignment P2 for Course Advanced Communication Networks & Systems at the University of St Andrews.

## Overview

This application implements a decentralized file-sharing system where nodes discover each other over IPv6 multicast, advertise their services, search for files across the network, and download files using TCP/IPv6 connections.

### Key Features

- **Node Discovery**: Nodes advertise themselves periodically via IPv6 multicast
- **Service Advertisement**: Each node announces its capabilities (search/download support)
- **Distributed Search**: Search queries are multicast to all nodes; results are returned via unicast
- **Reliable File Transfer**: Files are transferred using TCP/IPv6
- **mFQDNS Extension**: Caches and resolves FQDN to IPv6 address mappings
- **File Type Matching**: Supports path, filename, and substring-based file searching

Details of protocol design, see [protocol_specification.txt](./protocol_specification.txt).

## Getting Started

### Configuration 
Edit `config/configuration.properties`.

### Compile:
Change directory to code, and use command line:
```bash
find src -name "*.java" | xargs javac -d out
```

### Running
```bash
java -cp out cs4105p2.FileTreeBrowser
```

## Usage

Commands:
- `.` - List current directory
- `..` - Move up directory
- `:nodes` - List discovered nodes
- `:search` - Search files
- `:download` - Download file
- `:services` - Show available services
- `:quit` - Exit application

## Project Structure

```
code
├── README.md 					 # Project overview and instructions
├── config                       # Configuration files for the project
├── cs4105_p2_protocol_specification.txt  		# Protocol specification details
├── downloads                    # Folder for downloaded files
├── logs                         # Logs generated during execution
├── root_dir                     # Testing share dir
└── src                          # Source code for the application

```