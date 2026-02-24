package ru.vspochernin.errapi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/errors")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('READER', 'ADMIN', 'OWNER')")
public class ErrorsController {

    // TODO: inject errorsService.

    // TODO: implement /filters.

    // TODO: implement /events.
}
