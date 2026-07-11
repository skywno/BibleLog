from __future__ import annotations

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(KeyError)
    async def not_found_handler(_request: Request, exc: KeyError) -> JSONResponse:
        detail = str(exc) if str(exc) else "Resource not found"
        return JSONResponse(status_code=404, content={"detail": detail})

    @app.exception_handler(PermissionError)
    async def forbidden_handler(_request: Request, exc: PermissionError) -> JSONResponse:
        detail = str(exc) if str(exc) else "Forbidden"
        return JSONResponse(status_code=403, content={"detail": detail})

    @app.exception_handler(ValueError)
    async def bad_request_handler(_request: Request, exc: ValueError) -> JSONResponse:
        return JSONResponse(status_code=400, content={"detail": str(exc)})
