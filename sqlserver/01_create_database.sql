-- ============================================================
-- TechHub 数据库创建脚本 (SQL Server 2022)
-- 文件: 01_create_database.sql
-- ============================================================

IF DB_ID('techhub') IS NULL
    CREATE DATABASE [techhub];
GO

USE [techhub];
GO
