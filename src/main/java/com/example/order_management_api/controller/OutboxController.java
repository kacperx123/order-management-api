package com.example.order_management_api.controller;

import com.example.order_management_api.api.OutboxEventResponse;
import com.example.order_management_api.outbox.OutboxEvent;
import com.example.order_management_api.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/outbox-events")
@RequiredArgsConstructor
public class OutboxController {

    private final OutboxEventRepository outboxEventRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public List<OutboxEventResponse> listOutboxEvents(
            @RequestParam(defaultValue = "false") boolean unpublished
    ) {
        List<OutboxEvent> events = unpublished
                ? outboxEventRepository.findByPublishedAtIsNullOrderByOccurredAtAsc(PageRequest.of(0, 100)).getContent()
                : outboxEventRepository.findAll();

        return events.stream()
                .map(this::toResponse)
                .toList();
    }

    private OutboxEventResponse toResponse(OutboxEvent event) {
        return new OutboxEventResponse(
                event.getId(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getType(),
                event.getPayloadJson(),
                event.getOccurredAt(),
                event.getCreatedAt(),
                event.getPublishedAt()
        );
    }
}
