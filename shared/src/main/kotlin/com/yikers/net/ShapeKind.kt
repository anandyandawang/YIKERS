package com.yikers.net

// Draw primitive for a snapshot entity. Lives in shared so the server (RenderShape
// component) and the client (SnapshotRenderer) agree on it without importing each
// other's types.
enum class ShapeKind { CIRCLE, RECT }
