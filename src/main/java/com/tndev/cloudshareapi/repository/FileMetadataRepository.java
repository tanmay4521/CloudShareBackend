package com.tndev.cloudshareapi.repository;

import com.tndev.cloudshareapi.document.FileMetadataDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileMetadataRepository extends MongoRepository<FileMetadataDocument, String> {
    List<FileMetadataDocument> findByClerkId(String clerkId);
    Long countByClerkId(String clerkId);
}
