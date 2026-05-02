package com.example.cloudcli.service.storage;

import com.example.cloudcli.exception.StorageException;

import java.io.InputStream;
import java.nio.file.Path;

public interface StorageProvider {

    void store(Path localFile,String destination) throws StorageException;
    InputStream retrieve(String destination) throws StorageException;
}
