package com.wildfire.main.cloud;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

record QueuedFetch(UUID uuid, CompletableFuture<@Nullable JsonObject> future) {
}
