INSERT INTO public.notification_template (type, template)
VALUES
  ('PROJECT_INVITED', '{creator}님이 {project} 프로젝트에 회원님을 {position}포지션으로 초대했습니다.'),
  ('PROJECT_EXPELLED', '{project} 프로젝트에서 회원님을 내보냈습니다. 해당 프로젝트의 일정과 정보는 더 이상 보이지 않습니다.'),
  ('PROJECT_DELETED', '{project} 프로젝트가 삭제되었습니다. 해당 프로젝트의 일정과 정보는 더 이상 보이지 않습니다.'),
  ('SCHEDULE_CREATED', '{project}프로젝트에 새로운 {schedule}일정이 추가되었습니다.'),
  ('SCHEDULE_UPDATED', '{project}프로젝트에 {schedule}일정이 업데이트되었습니다.'),
  ('SCHEDULE_DELETED', '{project}프로젝트의 {schedule} 일정이 삭제되었습니다.'),
  ('INVITATION_PROCESSED', '회원님이 {receiver}님께 보낸 {project} 프로젝트 초대 요청이 {status}되었습니다.');