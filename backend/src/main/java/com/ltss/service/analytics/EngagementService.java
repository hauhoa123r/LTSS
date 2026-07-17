package com.ltss.service.analytics;

import com.ltss.common.exception.*;
import com.ltss.dto.analytics.*;
import com.ltss.entity.analytics.*;
import com.ltss.repository.analytics.*;
import com.ltss.entity.content.*;
import com.ltss.repository.content.*;
import com.ltss.entity.tour.*;
import java.time.*;
import java.util.*;

public interface EngagementService {

    EngagementAcceptedResponse record(EngagementEventRequest request);
}
