# 🩸 Rakt Sanchar

A blood donation management platform built using **Spring Boot** and **MongoDB** that helps manage blood donors and blood requests through REST APIs.

The application provides functionality for:

- Registering blood donors
- Creating blood requests
- Retrieving registered donors
- Retrieving blood requests
- Finding donors based on blood group and city
- Persisting application data in MongoDB
- Testing and validating APIs using Postman

---

## 📌 Table of Contents

- [Overview](#-overview)
- [Problem Statement](#-problem-statement)
- [Features](#-features)
- [Technology Stack](#-technology-stack)
- [System Architecture](#-system-architecture)
- [Project Structure](#-project-structure)
- [How the Application Works](#-how-the-application-works)
- [Database Design](#-database-design)
- [REST API Documentation](#-rest-api-documentation)
- [Donor Registration](#1-register-a-donor)
- [Get All Donors](#2-get-all-donors)
- [Find Donors](#3-find-donors-by-blood-group-and-city)
- [Create Blood Request](#4-create-a-blood-request)
- [Get Blood Requests](#5-get-all-blood-requests)
- [MongoDB Integration](#-mongodb-integration)
- [Spring Boot Concepts Used](#-spring-boot-concepts-used)
- [Testing with Postman](#-testing-with-postman)
- [Setup and Installation](#-setup-and-installation)
- [Running the Application](#-running-the-application)
- [Example Workflow](#-example-workflow)
- [Error Handling and Validation](#-error-handling-and-validation)
- [Current Limitations](#-current-limitations)
- [Future Improvements](#-future-improvements)
- [Learning Outcomes](#-learning-outcomes)
- [Interview Explanation](#-interview-explanation)
- [License](#-license)

---

# 📖 Overview

**Rakt Sanchar** is a backend-oriented blood donation platform designed to simplify the management of blood donors and blood requests.

The application is developed using **Java and Spring Boot** and uses **MongoDB** for persistent data storage.

The system currently supports two major entities:

1. **Donor**
2. **Blood Request**

A donor can register their basic information such as:

- Name
- Blood group
- City
- Phone number
- Age

A blood request can contain:

- Patient name
- Blood group
- Hospital
- City
- Urgency

The application exposes REST APIs that allow clients such as a frontend application or Postman to interact with the backend.

---

# 🎯 Problem Statement

Finding a suitable blood donor during an emergency can be difficult, especially when the required blood group is not readily available.

Rakt Sanchar aims to provide a simple platform where:

- Donors can register themselves.
- Blood requirements can be recorded.
- Users can retrieve donor information.
- Donors can be searched based on blood group and city.

The current implementation focuses on providing a clean backend API and persistent database storage.

---

# ✨ Features

## 👤 Donor Management

The application allows users to:

- Register a new donor.
- Retrieve all registered donors.
- Search donors using:
  - Blood group
  - City

---

## 🩸 Blood Request Management

Users can:

- Create a blood request.
- Store patient information.
- Specify the required blood group.
- Specify hospital and city.
- Specify urgency.
- Retrieve existing blood requests.

---

## 🔎 Donor Search

The application supports donor discovery using:

```text
Blood Group + City

                    Client
                      |
                      |
                HTTP Request
                      |
                      ↓
          ┌──────────────────────┐
          │  Spring Boot API     │
          │     Controller       │
          └──────────┬───────────┘
                     |
                     ↓
          ┌──────────────────────┐
          │   MongoRepository    │
          └──────────┬───────────┘
                     |
                     ↓
          ┌──────────────────────┐
          │      MongoDB         │
          │                      │
          │  donors              │
          │  blood_requests      │
          └──────────────────────┘

REQUEST FLOW

Postman / Frontend
        ↓
POST /api/donors
        ↓
DonorController
        ↓
DonorRepository
        ↓
MongoDB
        ↓
Saved Donor
        ↓
JSON Response

Project Structure
The important backend structure is:
src/
└── main/
    └── java/
        └── com/
            └── example/
                └── blood/
                    │
                    ├── controller/
                    │   ├── DonorController.java
                    │   └── BloodRequestController.java
                    │
                    ├── model/
                    │   ├── Donor.java
                    │   └── BloodRequest.java
                    │
                    └── repository/
                        ├── DonorRepository.java
                        └── BloodRequestRepository.java


This project currently focuses on the core backend functionality.
The implementation intentionally does not claim features such as:
- GPS-based donor tracking
- JWT authentication
- Advanced role management
- Real-time notifications
- Service-layer architecture
- Advanced MongoDB aggregation
- Production-scale deployment
These can be added as future improvements.



### One important thing for your interview

This README is also useful as your **interview preparation document**. Don't just memorize the project description. You should be able to explain this chain clearly:

**POST request → `DonorController` → `@RequestBody` → `DonorRepository` → `MongoRepository.save()` → MongoDB → JSON response**

And for searching:

**Query parameters → `@RequestParam` → derived repository query → MongoDB → matching donors**

If you can explain those two flows confidently, you'll be able to handle a lot of the **deep-dive questions** on Rakt Sanchar.
