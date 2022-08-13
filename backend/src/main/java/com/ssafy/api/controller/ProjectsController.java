package com.ssafy.api.controller;

import com.ssafy.DTO.project.ProjectSimpleInfoDTO;
import com.ssafy.DTO.user.UserSimpleInfoDTO;
import com.ssafy.api.request.AddDelUserInProjectPostReq;
import com.ssafy.api.request.DrawingUploadReq;
import com.ssafy.api.request.ProjectPatchPostReq;
import com.ssafy.api.request.ProjectsCreatePostReq;
import com.ssafy.api.response.ProjectInfoRes;
import com.ssafy.api.response.ProjectSimpleInfoRes;
import com.ssafy.api.service.*;
import com.ssafy.common.auth.SsafyUsersDetails;
import com.ssafy.common.customException.*;
import com.ssafy.common.model.response.BaseResponseBody;
import com.ssafy.db.entity.Conferences;
import com.ssafy.db.entity.DrawingImgPath;
import com.ssafy.db.entity.Projects;
import com.ssafy.db.entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.io.IOException;
import java.util.List;

import static org.kurento.jsonrpc.client.JsonRpcClient.log;

/**
 * Demmet에 사용되는 프로젝트 관리 컨트롤러
 * Projects테이블 등을 사용한다.
 */
@RestController
@RequestMapping("/projects")
public class ProjectsController {
    @Autowired
    UsersService usersService;

    @Autowired
    ProjectsService projectsService;

    @Autowired
    ConferencesService conferencesService;

    @Autowired
    UserProjectService usersProjectService;

    @Autowired
    AwsS3Service awsS3Service;

    @PostMapping()
    public ResponseEntity<BaseResponseBody> createProject(@ApiIgnore Authentication authentication, @RequestBody ProjectsCreatePostReq projectsCreatePostReq) {
        SsafyUsersDetails ssafyUsersDetails = (SsafyUsersDetails) authentication.getDetails();
        log.info("프로젝트 생성");
        try {
            projectsCreatePostReq.setOwner_id(ssafyUsersDetails.getUserUid());
            Projects savedProject = projectsService.createProject(projectsCreatePostReq);
            return ResponseEntity.status(200).body(BaseResponseBody.of(200, "success to make project"));
        } catch (UidNullException e) {
            log.error("프로젝트 생성중 맞지않는 uid발견.");
            return ResponseEntity.status(422).body(BaseResponseBody.of(422, e.getMessage()));
        }
    }

//    @PatchMapping("/deactivate")
//    public ResponseEntity<BaseResponseBody> deactivateProject(Authentication authentication, @RequestBody Map<String, Integer> request) {
//        SsafyUsersDetails ssafyUsersDetails = (SsafyUsersDetails) authentication.getDetails();
//        log.info("프로젝트 비활성화");
//        long pid = request.get("pid");
//        try {
//            Projects project = projectsService.deactivateProject(pid, ssafyUsersDetails.getUserUid());
//        } catch (ProjectNullException | NoAuthorizedException e) {
//            return ResponseEntity.status(422).body(BaseResponseBody.of(200, e.getMessage()));
//        }
//        return ResponseEntity.status(200).body(BaseResponseBody.of(200, "success to deactivate " + pid + " project"));
//    }

    @GetMapping("/{pid}")
    public ResponseEntity<BaseResponseBody> getProject(Authentication authentication, @PathVariable Long pid) {
        SsafyUsersDetails ssafyUsersDetails = (SsafyUsersDetails) authentication.getDetails();
        try {
            log.info("pid를 기반으로 특정 프로젝트 상세조회");
            log.debug("pid = {}", pid);

            log.info("pid를 기반으로 프로젝트 조회");
            Projects project = projectsService.getProject(pid);
            log.debug("프로젝트 = {}", project.toString());

            log.info("userprojects테이블에서 pid를 가진 유저들을 모조리 가지고온다.");
            List<UserSimpleInfoDTO> userList = usersProjectService.getUserSimpleInfoDTOListByPid(pid);
            log.debug("userList = {}", userList.toString());

            log.info("모든 값을 받았으니 리턴해준다.");
            return ResponseEntity.status(200).body(ProjectInfoRes.of(200, "success to find project details", project, project.getOwnerId(), userList));
        } catch (ProjectNullException e) {
            log.error("pid로 프로젝트를 찾지 못함");
            return ResponseEntity.status(422).body(BaseResponseBody.of(422, e.getMessage()));
        } catch (UidNullException e) {
            log.error("pid를 기반으로 userProject테이블에서 user를 찾지 못함");
            return ResponseEntity.status(422).body(BaseResponseBody.of(422, e.getMessage()));
        }
    }


    @GetMapping("/activate/joined")
    public ResponseEntity<BaseResponseBody> getJoindActivateProjects(Authentication authentication) {
        SsafyUsersDetails ssafyUsersDetails = (SsafyUsersDetails) authentication.getDetails();
        Long uid = ssafyUsersDetails.getUserUid();
        System.out.println(uid);
        try {
            List<ProjectSimpleInfoDTO> projectList = projectsService.getJoinedProjectList(uid);
            for (int i = 0; i < projectList.size(); i++) {
                //
                List<UserSimpleInfoDTO> userList = usersProjectService.getUserSimpleInfoDTOListByPid(projectList.get(i).getPid());
                projectList.get(i).setMember(userList);
            }
            return ResponseEntity.status(200).body(ProjectSimpleInfoRes.of(200, "success", projectList));
        } catch (ProjectNullException e) {
            return ResponseEntity.status(422).body(BaseResponseBody.of(422, e.getMessage()));
        } catch (UidNullException e) {
            // db가 꼬일경우만 발생할듯함
            // pid를 기반으로 userProject테이블에서 user를 찾을때 없으면 발생하는 오류
            return ResponseEntity.status(422).body(BaseResponseBody.of(422, e.getMessage()));
        }
    }

    @GetMapping("/activate")
    public ResponseEntity<BaseResponseBody> getActivateProjects(Authentication authentication) {
        SsafyUsersDetails ssafyUsersDetails = (SsafyUsersDetails) authentication.getDetails();
        Long uid = ssafyUsersDetails.getUserUid();
        System.out.println(uid);
        try {
            List<ProjectSimpleInfoDTO> projectList = projectsService.getActivateProjectsList(uid);
            for (int i = 0; i < projectList.size(); i++) {
                List<UserSimpleInfoDTO> userList = usersProjectService.getUserSimpleInfoDTOListByPid(projectList.get(i).getPid());
                projectList.get(i).setMember(userList);
            }
            return ResponseEntity.status(200).body(ProjectSimpleInfoRes.of(200, "success", projectList));
        } catch (ProjectNullException e) {
            return ResponseEntity.status(422).body(BaseResponseBody.of(422, e.getMessage()));
        } catch (UidNullException e) {
            // db가 꼬일경우만 발생할듯함
            // pid를 기반으로 userProject테이블에서 user를 찾을때 없으면 발생하는 오류
            return ResponseEntity.status(422).body(BaseResponseBody.of(422, e.getMessage()));
        }
    }

    @PatchMapping()
    public ResponseEntity<BaseResponseBody> patchProjectInfo(Authentication authentication, @RequestBody ProjectPatchPostReq projectPatchPostReq) {
        SsafyUsersDetails ssafyUsersDetails = (SsafyUsersDetails) authentication.getDetails();
        Long uid = ssafyUsersDetails.getUserUid();
        try {
            Projects changedProject = projectsService.patchProjectInfo(projectPatchPostReq, uid);
            return ResponseEntity.status(200).body(BaseResponseBody.of(200, "success"));
        } catch (ProjectNullException e) {
            return ResponseEntity.status(422).body(BaseResponseBody.of(422, e.getMessage()));
        } catch (NullPointerException e) {
            return ResponseEntity.status(422).body(BaseResponseBody.of(422, "Either name or desc or deactivate must not be null."));
        }
    }

    @PostMapping("/user")
    public ResponseEntity<BaseResponseBody> addUserInProject(Authentication authentication, @RequestBody AddDelUserInProjectPostReq addDelUserInProjectPostReq) {
        SsafyUsersDetails ssafyUsersDetails = (SsafyUsersDetails) authentication.getDetails();
        Long uid = ssafyUsersDetails.getUserUid();
        try {
            // 내가 해당 pid를 가지는 프로젝트의 오너일 경우에만 유저추가가 가능하기때문에 일단 그 여부부터 확인한다.
            Projects project = projectsService.getProject(addDelUserInProjectPostReq.getPid());
            // 같지않다면
            if (!project.getOwnerId().equals(uid))
                return ResponseEntity.status(422).body(BaseResponseBody.of(422, "You do not have permission."));
            // 해당하는 uid를 가지는 유저가 Users에 있는지 체크
            Users user = usersService.getUsersByUid(addDelUserInProjectPostReq.getUid());
            // 해당하는 uid를 가지는 유저가 프로젝트에 이미 추가되어있는지 확인
            // true면 중복됨, false면 중복 없음
            if (usersProjectService.userDuplicateCheck(project, user)) {
                return ResponseEntity.status(200).body(BaseResponseBody.of(422, "member duplicate"));
            }
            // 두 조건 모두 완료하면 실제 추가직업 진행
            usersProjectService.addUserInProject(addDelUserInProjectPostReq, project, user);
            return ResponseEntity.status(200).body(BaseResponseBody.of(200, "success"));

        } catch (ProjectNullException e) {
            return ResponseEntity.status(422).body(BaseResponseBody.of(422, e.getMessage()));
        } catch (UidNullException e) {
            return ResponseEntity.status(422).body(BaseResponseBody.of(422, e.getMessage()));
        } catch (UserNullException e) {
            throw new RuntimeException(e);
        }
    }

    @DeleteMapping("/user")
    public ResponseEntity<BaseResponseBody> deleteUserInProject(Authentication authentication, @RequestBody AddDelUserInProjectPostReq addDelUserInProjectPostReq) {
        SsafyUsersDetails ssafyUsersDetails = (SsafyUsersDetails) authentication.getDetails();
        Long uid = ssafyUsersDetails.getUserUid();
        try {
            Projects project = projectsService.getProject(addDelUserInProjectPostReq.getPid());
            Users user = usersService.getUsersByUid(addDelUserInProjectPostReq.getUid());
            usersProjectService.deleteUserInProject(project, user);
        } catch (ProjectNullException e) {
            throw new RuntimeException(e);
        } catch (UidNullException e) {
            throw new RuntimeException(e);
        } catch (UserNullException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.status(200).body(BaseResponseBody.of(200, "success"));
    }

    // 드로잉 이미지 업로드
    @PostMapping("/drawing")
    public ResponseEntity<BaseResponseBody> uploadProjectDrawing(@ApiIgnore Authentication authentication, @ModelAttribute DrawingUploadReq drawingUploadReq) {
        SsafyUsersDetails ssafyUsersDetails = (SsafyUsersDetails) authentication.getDetails();
        Long uid = ssafyUsersDetails.getUserUid();
        String openviduSessionId = drawingUploadReq.getOpenviduSessionId();
        log.info("enter uploadProjectDrawing in ProjectsController");
        log.debug("openviduSessionId = {}", drawingUploadReq.getOpenviduSessionId());
//        받은 openviduSessionId를 기반으로 이미지 저장할 예정이므로 다음과 같은 객체들을 가져온다.
//        openviduSessionId를 가지고있는 conferences
//        생성자의 Users
        Conferences conference = null;
        Users user = null;
        String path = null;
        DrawingImgPath drawingImgPath = null;
        try {
            conference = conferencesService.findConferencesBySessionNameAndActivation(openviduSessionId);
            user = usersService.getUsersByUid(uid);
            path = awsS3Service.putImage(drawingUploadReq.getMultipartFile(), conference.getProject().getPid(), "drawing");
//            이제 db에 정보 넣기
            drawingImgPath = awsS3Service.saveDrawingImagePath(path, conference, user, "drawing");
            log.debug("saveDrawingImagePath = {}", drawingImgPath.toString());
        } catch (ConferenceNullException e) {
            throw new RuntimeException(e);
        } catch (UserNullException e) {
            throw new RuntimeException(e);
        } catch (UidNullException e) {
            throw new RuntimeException(e);
        } catch (NotImageException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.status(200).body(BaseResponseBody.of(200, path));
    }
}
