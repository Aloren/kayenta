/*
 * Copyright 2017 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.kayenta.controllers;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.netflix.kayenta.canary.CanaryArchiveResultUpdateResponse;
import com.netflix.kayenta.canary.CanaryExecutionStatusResponse;
import com.netflix.kayenta.security.AccountCredentials;
import com.netflix.kayenta.security.AccountCredentialsRepository;
import com.netflix.kayenta.storage.ObjectType;
import com.netflix.kayenta.storage.StorageService;
import com.netflix.kayenta.storage.StorageServiceRepository;
import com.netflix.spinnaker.kork.web.exceptions.NotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/canaryResultArchive")
@Api(
    value = "/canaryResultArchive",
    description =
        "Manipulate the archived canary result object store.  This should be used only for Kayenta maintenance.  Use the /canary endpoints for canary results.")
@Slf4j
public class CanaryResultArchiveController {

  private final AccountCredentialsRepository accountCredentialsRepository;
  private final StorageServiceRepository storageServiceRepository;

  @Autowired
  public CanaryResultArchiveController(
      AccountCredentialsRepository accountCredentialsRepository,
      StorageServiceRepository storageServiceRepository) {
    this.accountCredentialsRepository = accountCredentialsRepository;
    this.storageServiceRepository = storageServiceRepository;
  }

  @ApiOperation(value = "Retrieve an archived canary result from object storage")
  @GetMapping(value = "/{pipelineId:.+}")
  public CanaryExecutionStatusResponse loadArchivedCanaryResult(
      @RequestParam(required = false) final String storageAccountName,
      @PathVariable String pipelineId) {
    String resolvedConfigurationAccountName = resolveStorageAccountName(storageAccountName);
    StorageService storageService = getStorageService(resolvedConfigurationAccountName);

    return storageService.loadObject(
        resolvedConfigurationAccountName, ObjectType.CANARY_RESULT_ARCHIVE, pipelineId);
  }

  @ApiOperation(value = "Create an archived canary result to object storage")
  @PostMapping(consumes = APPLICATION_JSON_VALUE)
  public CanaryArchiveResultUpdateResponse storeArchivedCanaryResult(
      @RequestParam(required = false) final String storageAccountName,
      @RequestParam(required = false) String pipelineId,
      @RequestBody CanaryExecutionStatusResponse canaryExecutionStatusResponse)
      throws IOException {
    String resolvedConfigurationAccountName = resolveStorageAccountName(storageAccountName);
    StorageService storageService = getStorageService(resolvedConfigurationAccountName);

    if (pipelineId == null) {
      pipelineId = UUID.randomUUID() + "";
    }

    try {
      storageService.loadObject(
          resolvedConfigurationAccountName, ObjectType.CANARY_RESULT_ARCHIVE, pipelineId);
    } catch (NotFoundException e) {
      storageService.storeObject(
          resolvedConfigurationAccountName,
          ObjectType.CANARY_RESULT_ARCHIVE,
          pipelineId,
          canaryExecutionStatusResponse,
          pipelineId + ".json",
          false);

      return CanaryArchiveResultUpdateResponse.builder().pipelineId(pipelineId).build();
    }

    throw new IllegalArgumentException(
        "Archived canary result '" + pipelineId + "' already exists.");
  }

  @ApiOperation(value = "Update an archived canary result in object storage")
  @PutMapping(value = "/{pipelineId:.+}", consumes = APPLICATION_JSON_VALUE)
  public CanaryArchiveResultUpdateResponse updateArchivedCanaryResult(
      @RequestParam(required = false) final String storageAccountName,
      @PathVariable String pipelineId,
      @RequestBody CanaryExecutionStatusResponse canaryExecutionStatusResponse)
      throws IOException {
    String resolvedConfigurationAccountName = resolveStorageAccountName(storageAccountName);
    StorageService storageService = getStorageService(resolvedConfigurationAccountName);

    try {
      storageService.loadObject(
          resolvedConfigurationAccountName, ObjectType.CANARY_RESULT_ARCHIVE, pipelineId);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Archived canary result '" + pipelineId + "' does not exist.");
    }

    storageService.storeObject(
        resolvedConfigurationAccountName,
        ObjectType.CANARY_RESULT_ARCHIVE,
        pipelineId,
        canaryExecutionStatusResponse,
        pipelineId + ".json",
        true);

    return CanaryArchiveResultUpdateResponse.builder().pipelineId(pipelineId).build();
  }

  @ApiOperation(value = "Delete an archived canary result from object storage")
  @DeleteMapping(value = "/{pipelineId:.+}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteArchivedCanaryResult(
      @RequestParam(required = false) final String storageAccountName,
      @PathVariable String pipelineId) {
    String resolvedConfigurationAccountName = resolveStorageAccountName(storageAccountName);
    StorageService storageService = getStorageService(resolvedConfigurationAccountName);

    storageService.deleteObject(
        resolvedConfigurationAccountName, ObjectType.CANARY_RESULT_ARCHIVE, pipelineId);
  }

  @ApiOperation(value = "Retrieve a list of archived canary result ids in object storage")
  @GetMapping
  public List<Map<String, Object>> listAllCanaryArchivedResults(
      @RequestParam(required = false) final String storageAccountName) {
    String resolvedConfigurationAccountName = resolveStorageAccountName(storageAccountName);
    StorageService storageService = getStorageService(resolvedConfigurationAccountName);

    return storageService.listObjectKeys(
        resolvedConfigurationAccountName, ObjectType.CANARY_RESULT_ARCHIVE);
  }

  private String resolveStorageAccountName(String storageAccountName) {
    return accountCredentialsRepository
        .getRequiredOneBy(storageAccountName, AccountCredentials.Type.OBJECT_STORE)
        .getName();
  }

  private StorageService getStorageService(String resolvedStorageAccountName) {
    return storageServiceRepository.getRequiredOne(resolvedStorageAccountName);
  }
}
