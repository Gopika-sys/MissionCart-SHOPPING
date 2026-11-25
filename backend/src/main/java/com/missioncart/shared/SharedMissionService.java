package com.missioncart.shared;

import com.missioncart.missions.*;
import com.missioncart.missions.MissionDtos.MissionDetailDto;
import com.missioncart.security.AuthenticatedUser;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class SharedMissionService {

    public record SharedRoomDto(
        String shareToken,
        MissionDetailDto mission,
        List<MissionMember> members,
        List<MissionContribution> contributions,
        List<ActivityLog> activityHistory,
        String contributionNote
    ) {}

    private final MissionRepository missionRepo;
    private final MissionItemRepository itemRepo;
    private final MissionMemberRepository memberRepo;
    private final MissionInvitationRepository invitationRepo;
    private final MissionContributionRepository contributionRepo;
    private final ActivityLogRepository logRepo;
    private final MissionService missionService;
    private final SimpMessagingTemplate messagingTemplate;

    public SharedMissionService(
        MissionRepository missionRepo,
        MissionItemRepository itemRepo,
        MissionMemberRepository memberRepo,
        MissionInvitationRepository invitationRepo,
        MissionContributionRepository contributionRepo,
        ActivityLogRepository logRepo,
        MissionService missionService,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.missionRepo = missionRepo;
        this.itemRepo = itemRepo;
        this.memberRepo = memberRepo;
        this.invitationRepo = invitationRepo;
        this.contributionRepo = contributionRepo;
        this.logRepo = logRepo;
        this.missionService = missionService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public SharedRoomDto createShareLink(Long missionId, AuthenticatedUser owner) {
        Mission m = missionRepo.findById(missionId)
            .orElseThrow(() -> new EntityNotFoundException("Mission not found"));
        if (m.getUserId() != null && !m.getUserId().equals(owner.id())) {
            throw new IllegalArgumentException("Only the mission owner can enable sharing.");
        }
        if (m.getShareToken() == null || m.getShareToken().isBlank()) {
            m.setShareToken("mc-" + UUID.randomUUID().toString().substring(0, 10));
        }
        m.setShared(true);
        missionRepo.save(m);

        invitationRepo.findByMissionIdAndActiveTrue(missionId).orElseGet(() -> {
            MissionInvitation inv = new MissionInvitation();
            inv.setMissionId(missionId);
            inv.setToken(m.getShareToken());
            inv.setCreatedByUserId(owner.id());
            inv.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
            return invitationRepo.save(inv);
        });

        memberRepo.findByMissionIdAndUserId(missionId, owner.id()).orElseGet(() -> {
            MissionMember mm = new MissionMember();
            mm.setMissionId(missionId);
            mm.setUserId(owner.id());
            mm.setMemberName(owner.email().split("@")[0]);
            mm.setMemberEmail(owner.email());
            mm.setRole("OWNER");
            return memberRepo.save(mm);
        });

        logActivity(owner.email(), owner.role(), "SHARED_MISSION", String.valueOf(missionId), "SHARE_ENABLED", "Shared invitation link created");
        return getByToken(m.getShareToken());
    }

    public SharedRoomDto getByToken(String token) {
        Mission m = missionRepo.findByShareToken(token)
            .orElseThrow(() -> new EntityNotFoundException("Shared mission not found or link expired."));
        MissionDetailDto detail = missionService.getMissionDetail(m.getId(), null);
        return new SharedRoomDto(
            token,
            detail,
            memberRepo.findByMissionIdOrderByJoinedAtAsc(m.getId()),
            contributionRepo.findByMissionIdOrderByCreatedAtDesc(m.getId()),
            logRepo.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("SHARED_MISSION", String.valueOf(m.getId())),
            "Pending/demo contribution tracking (split payments are not charged automatically)."
        );
    }

    @Transactional
    public SharedRoomDto join(String token, AuthenticatedUser user, String displayName) {
        Mission m = missionRepo.findByShareToken(token)
            .orElseThrow(() -> new EntityNotFoundException("Shared mission not found."));
        String name = (displayName != null && !displayName.isBlank()) ? displayName.trim() : user.email().split("@")[0];
        memberRepo.findByMissionIdAndUserId(m.getId(), user.id()).orElseGet(() -> {
            MissionMember mm = new MissionMember();
            mm.setMissionId(m.getId());
            mm.setUserId(user.id());
            mm.setMemberName(name);
            mm.setMemberEmail(user.email());
            mm.setRole("MEMBER");
            return memberRepo.save(mm);
        });
        logActivity(user.email(), user.role(), "SHARED_MISSION", String.valueOf(m.getId()), "MEMBER_JOINED", name + " joined the group mission");
        SharedRoomDto room = getByToken(token);
        messagingTemplate.convertAndSend("/topic/shared/" + token, room);
        return room;
    }

    @Transactional
    public SharedRoomDto updateSharedItem(String token, Long itemId, AuthenticatedUser user, String action, String claimName, Long replaceProductId) {
        Mission m = missionRepo.findByShareToken(token)
            .orElseThrow(() -> new EntityNotFoundException("Shared mission not found."));
        MissionItem item = itemRepo.findById(itemId)
            .orElseThrow(() -> new EntityNotFoundException("Mission item not found."));
        if (!item.getMissionId().equals(m.getId())) {
            throw new IllegalArgumentException("Item does not belong to this mission.");
        }

        if ("CLAIM".equalsIgnoreCase(action)) {
            item.setAssignedToUserId(user.id());
            item.setClaimedByName((claimName != null && !claimName.isBlank()) ? claimName.trim() : user.email().split("@")[0]);
            itemRepo.save(item);
            logActivity(user.email(), user.role(), "SHARED_MISSION", String.valueOf(m.getId()), "ITEM_CLAIMED",
                item.getClaimedByName() + " claimed " + item.getRequirementLabel());
        } else if ("UNCLAIM".equalsIgnoreCase(action)) {
            item.setAssignedToUserId(null);
            item.setClaimedByName(null);
            itemRepo.save(item);
            logActivity(user.email(), user.role(), "SHARED_MISSION", String.valueOf(m.getId()), "ITEM_UNCLAIMED",
                "Unclaimed " + item.getRequirementLabel());
        } else if ("REPLACE".equalsIgnoreCase(action) && replaceProductId != null) {
            missionService.updateMission(m.getId(), m.getUserId(), new MissionDtos.UpdateMissionRequest(
                null, null, null, List.of(new MissionDtos.UpdateMissionItemAction(itemId, null, null, null, replaceProductId, null, null, null))
            ));
            logActivity(user.email(), user.role(), "SHARED_MISSION", String.valueOf(m.getId()), "ITEM_REPLACED",
                "Updated product selection for " + item.getRequirementLabel());
        }

        SharedRoomDto room = getByToken(token);
        messagingTemplate.convertAndSend("/topic/shared/" + token, room);
        return room;
    }

    @Transactional
    public SharedRoomDto recordDemoContribution(String token, AuthenticatedUser user, BigDecimal amount, String note) {
        Mission m = missionRepo.findByShareToken(token)
            .orElseThrow(() -> new EntityNotFoundException("Shared mission not found."));
        MissionMember member = memberRepo.findByMissionIdAndUserId(m.getId(), user.id()).orElse(null);

        MissionContribution contrib = new MissionContribution();
        contrib.setMissionId(m.getId());
        contrib.setMemberId(member != null ? member.getId() : null);
        contrib.setAmount(amount);
        contrib.setCurrency("INR");
        contrib.setStatus("DEMO_PENDING");
        contrib.setNote(note != null ? note : "Pledged contribution (Demo workflow)");
        contributionRepo.save(contrib);

        logActivity(user.email(), user.role(), "SHARED_MISSION", String.valueOf(m.getId()), "CONTRIBUTION_LOGGED",
            "Logged ₹" + amount + " demo pledge");
        SharedRoomDto room = getByToken(token);
        messagingTemplate.convertAndSend("/topic/shared/" + token, room);
        return room;
    }

    public List<MissionDetailDto> listSharedMissionsForUser(Long userId) {
        return missionRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .filter(Mission::isShared)
            .map(m -> missionService.getMissionDetail(m.getId(), userId))
            .toList();
    }

    private void logActivity(String email, String role, String entityType, String entityId, String action, String details) {
        ActivityLog log = new ActivityLog();
        log.setActorEmail(email);
        log.setActorRole(role);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setDetails(details);
        logRepo.save(log);
    }
}
