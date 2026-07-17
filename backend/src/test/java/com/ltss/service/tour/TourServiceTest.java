package com.ltss.service.tour;

import com.ltss.service.tour.impl.TourServiceImpl;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.repository.auth.UserRepository;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.service.auth.AuditService;
import com.ltss.repository.place.PlaceRepository;
import com.ltss.dto.tour.*;
import com.ltss.entity.tour.*;
import com.ltss.repository.tour.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Clock;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TourServiceTest {
    @Mock TourRepository tourRepository;
    @Mock TourItemRepository itemRepository;
    @Mock PlaceRepository placeRepository;
    @Mock UserRepository userRepository;
    @Mock CurrentUserService currentUserService;
    @Mock AuditService auditService;
    TourService service;

    @BeforeEach
    void setUp() {
        service = new TourServiceImpl(tourRepository, itemRepository, placeRepository, userRepository,
                currentUserService, auditService, Clock.systemUTC());
        lenient().when(currentUserService.requireUserId()).thenReturn(10L);
    }

    @Test
    void destinationsMustBeUnique() {
        TourUpsertRequest request = request(0, List.of(item(2L), item(2L)));
        assertThrows(ConflictException.class, () -> service.create(request, null));
        verify(tourRepository, never()).save(any());
    }

    @Test
    void staleVersionStopsUpdateBeforeReplacingItems() {
        TourEntity tour = mock(TourEntity.class);
        when(tour.getOwnerUserId()).thenReturn(10L);
        when(tour.getStatus()).thenReturn(TourStatus.DRAFT);
        when(tour.getVersion()).thenReturn(3);
        when(tourRepository.findLockedById(8L)).thenReturn(Optional.of(tour));

        assertThrows(ConflictException.class, () -> service.update(8L, request(2, List.of(item(2L), item(3L))), null));
        verify(itemRepository, never()).deleteAllByTourId(anyLong());
    }

    @Test
    void privatePublishedTourCannotBeCopiedByAnotherUser() {
        TourEntity source = mock(TourEntity.class);
        when(source.getStatus()).thenReturn(TourStatus.PUBLISHED);
        when(source.getVisibility()).thenReturn(TourVisibility.PRIVATE);
        when(source.getOwnerUserId()).thenReturn(20L);
        when(tourRepository.findById(8L)).thenReturn(Optional.of(source));

        assertThrows(ResourceNotFoundException.class, () -> service.copy(8L, null));
        verify(itemRepository, never()).findAllByTourIdOrderByVisitOrderAsc(anyLong());
    }

    @Test
    void draftTourCannotBeShared() {
        TourEntity tour = mock(TourEntity.class);
        when(tour.getOwnerUserId()).thenReturn(10L);
        when(tour.getVersion()).thenReturn(1);
        when(tour.getStatus()).thenReturn(TourStatus.DRAFT);
        when(tourRepository.findLockedById(8L)).thenReturn(Optional.of(tour));

        assertThrows(ConflictException.class, () -> service.changeVisibility(8L,
                new ChangeTourVisibilityRequest(TourVisibility.PUBLIC, 1), null));
        verify(tour, never()).changeVisibility(any());
    }

    private TourUpsertRequest request(Integer version, List<TourItemRequest> items) {
        return new TourUpsertRequest("Tour Sơn Tây", null, null, null, null, null, items, version);
    }

    private TourItemRequest item(Long placeId) {
        return new TourItemRequest(placeId, null, null, null, null);
    }
}
