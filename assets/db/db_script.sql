DROP TABLE IF EXISTS EventSkills;
DROP TABLE IF EXISTS FinalReport;
DROP TABLE IF EXISTS Report;
DROP TABLE IF EXISTS HelpRequest;
DROP TABLE IF EXISTS Notification;
DROP TABLE IF EXISTS EventParticipants;
DROP TABLE IF EXISTS Events;
DROP TABLE IF EXISTS VolunteerSkills;
DROP TABLE IF EXISTS Skills;
DROP TABLE IF EXISTS VolunteerOrganization;
DROP TABLE IF EXISTS Admin;
DROP TABLE IF EXISTS PersonInNeed;
DROP TABLE IF EXISTS Volunteer;
DROP TABLE IF EXISTS SystemUser;

CREATE TABLE SystemUser (
    "username" TEXT NOT NULL PRIMARY KEY,
    "password" TEXT NOT NULL,
    "email" TEXT NULL,
    "phone" TEXT NULL,
    "address" TEXT NULL
);

CREATE TABLE Volunteer (
    "username" TEXT NOT NULL PRIMARY KEY,
    "fullName" TEXT NULL,
    "cccd" TEXT NULL,
    "dateOfBirth" DATE NULL,
    "averageRating" DOUBLE PRECISION NULL,
    "ratingCount" INTEGER NULL,
    "freeHourPerWeek" INTEGER NOT NULL,
    FOREIGN KEY (username) REFERENCES SystemUser(username)
);

CREATE TABLE PersonInNeed (
    "username" TEXT NOT NULL PRIMARY KEY,
    "fullName" TEXT NULL,
    "cccd" TEXT NULL,
    "dateOfBirth" DATE NULL,
    FOREIGN KEY (username) REFERENCES SystemUser(username)
);

CREATE TABLE Admin (
    "username" TEXT NOT NULL PRIMARY KEY,
    FOREIGN KEY (username) REFERENCES SystemUser(username)
);

CREATE TABLE VolunteerOrganization (
    "username" TEXT NOT NULL PRIMARY KEY,
    "organizationName" TEXT NULL,
    "licenseNumber" TEXT NULL,
    "field" TEXT NULL,
    "representative" TEXT NULL,
    "sponsor" TEXT NOT NULL,
    "info" TEXT NOT NULL,
    FOREIGN KEY (username) REFERENCES SystemUser(username)
);

CREATE TABLE Skills (
    "skillId" INTEGER NOT NULL PRIMARY KEY,
    "skill" TEXT NOT NULL
);

CREATE TABLE VolunteerSkills (
    "username" TEXT NOT NULL,
    "skillId" INTEGER NOT NULL,
    PRIMARY KEY (username, skillId),
    FOREIGN KEY (skillId) REFERENCES Skills(skillId),
    FOREIGN KEY (username) REFERENCES Volunteer(username)
);

CREATE TABLE Events (
    "eventId" INTEGER NOT NULL PRIMARY KEY,
    "title" TEXT NULL,
    "maxParticipantNumber" INTEGER NULL,
    "startDate" DATE NULL,
    "endDate" DATE NULL,
    "emergencyLevel" TEXT NULL,
    "description" TEXT NULL,
    "organizer" TEXT NOT NULL,
    "requestId" TEXT,
    "status" TEXT NULL DEFAULT 'Pending',
    FOREIGN KEY (requestId) REFERENCES HelpRequest(requestId),
    FOREIGN KEY (organizer) REFERENCES VolunteerOrganization(username)
);

CREATE TABLE EventParticipants (
    "eventId" INTEGER NOT NULL,
    "username" TEXT NOT NULL,
    "hoursParticipated" INTEGER NULL,
    "ratingByOrg" INTEGER NULL,
    PRIMARY KEY (eventId, username),
    FOREIGN KEY (eventId) REFERENCES Events(eventId),
    FOREIGN KEY (username) REFERENCES Volunteer(username)
);

CREATE TABLE Notification (
    "notificationId" INTEGER NOT NULL PRIMARY KEY,
    "eventId" INTEGER NULL,
    "username" TEXT NULL,
    "acceptStatus" TEXT NULL DEFAULT 'Pending',
    FOREIGN KEY (eventId) REFERENCES Events(eventId),
    FOREIGN KEY (username) REFERENCES Volunteer(username)
);

CREATE TABLE HelpRequest (
    "requestId" INTEGER PRIMARY KEY AUTOINCREMENT,
    "title" TEXT NULL,
    "startDate" DATE NULL,
    "emergencyLevel" TEXT NULL,
    "description" TEXT NULL,
    "personInNeedId" TEXT NULL,
    "status" TEXT NULL DEFAULT 'Pending',
    "contact" TEXT NULL,
    "address" TEXT NULL,
    FOREIGN KEY (personInNeedId) REFERENCES PersonInNeed(username)
);

CREATE TABLE Report (
    "reportId" INTEGER NOT NULL PRIMARY KEY,
    "eventId" INTEGER NULL,
    "reportDate" DATE NULL,
    "progress" INTEGER NULL,
    "note" TEXT NULL,
    FOREIGN KEY (eventId) REFERENCES Events(eventId)
);

CREATE TABLE FinalReport (
    "reportId" INTEGER NOT NULL PRIMARY KEY,
    FOREIGN KEY (reportId) REFERENCES Report(reportId)
);

CREATE TABLE EventSkills (
    eventID INTEGER NOT NULL,
    skillId INTEGER NOT NULL,
    PRIMARY KEY (eventID, skillId),
    FOREIGN KEY (eventID) REFERENCES Events(eventId),
    FOREIGN KEY (skillId) REFERENCES Skills(skillId)
);

-- Add admin user to SystemUser table
INSERT INTO SystemUser (username, password, email, phone, address)
VALUES ('admin', '123', 'admin@system.com', '0123456789', 'System Administration Office');

-- Then add the admin user to Admin table
INSERT INTO Admin (username)
VALUES ('admin');

-- =================================================================
-- ========================== SAMPLE DATA ==========================
-- =================================================================

-- 1. SystemUser entries
INSERT INTO SystemUser (username, password, email, phone, address) VALUES
('tnv1', '123', 'volunteer1@example.com', '0911111111', '123 Volunteer St, Hanoi'),
('tnv2', '123', 'volunteer2@example.com', '0922222222', '456 Volunteer Ave, HCMC'),
('nguoicanhotro1', '123', 'person1@example.com', '0933333333', '789 Needy Rd, Da Nang'),
('nguoicanhotro2', '123', 'person2@example.com', '0944444444', '101 Support Ln, Hue'),
('tochuc1', '123', 'org1@example.com', '0955555555', '202 Charity Blvd, Hanoi'),
('tochuc2', '123', 'org2@example.com', '0966666666', '303 Kindness Sq, HCMC');

-- 2. PersonInNeed entries
INSERT INTO PersonInNeed (username, fullName, cccd, dateOfBirth) VALUES
('nguoicanhotro1', 'Lê Văn Cần', '012345678901', '1955-08-10'),
('nguoicanhotro2', 'Phạm Thị Khó', '012345678902', '1960-12-25');

-- 3. Volunteer entries (with initial ratings)
INSERT INTO Volunteer (username, fullName, cccd, dateOfBirth, averageRating, ratingCount, freeHourPerWeek) VALUES
('tnv1', 'Nguyễn Thiện Nguyện', '012345678903', '1990-01-15', 8.0, 1, 20),
('tnv2', 'Trần Tốt Bụng', '012345678904', '1995-05-20', 9.0, 1, 15);

-- 4. VolunteerOrganization entries
INSERT INTO VolunteerOrganization (username, organizationName, licenseNumber, field, representative, sponsor, info) VALUES
('tochuc1', 'Vì Cộng Đồng', 'ORG-VCD-01', 'Education', 'Hoàng Văn E', 'Công ty ABC', 'Tổ chức hỗ trợ giáo dục cho trẻ em vùng sâu vùng xa.'),
('tochuc2', 'Ánh Dương', 'ORG-AD-02', 'Healthcare', 'Đỗ Thị F', 'Tập đoàn XYZ', 'Tổ chức hỗ trợ y tế cho người nghèo và người già neo đơn.');

-- 5. Skills library (in English to match UI)
INSERT INTO Skills (skillId, skill) VALUES
(1, 'Communication'), (2, 'First Aid'), (3, 'Education'), (4, 'Cooking'), (5, 'Driving'), (6, 'Fundraising');

-- 6. VolunteerSkills linking
INSERT INTO VolunteerSkills (username, skillId) VALUES
('tnv1', 1), ('tnv1', 2), ('tnv1', 5),
('tnv2', 3), ('tnv2', 4), ('tnv2', 1);

-- 7. HelpRequest entries (logical statuses based on Admin approvals)
-- Request #1: Closed, because its linked Event #1 is already Done.
-- Request #2: Approved, Admin approved the request, it's now open for Orgs to create events. No org has created an event for it yet.
-- Request #3: Closed, because an Org created Event #6 for it, and the Admin has approved that event.
-- Request #4: Rejected, Admin rejected the initial request.
-- Request #5: Pending, PIN created it, but Admin has not approved the request itself yet.
INSERT INTO HelpRequest (requestId, title, startDate, emergencyLevel, description, personInNeedId, status, contact, address) VALUES
(1, 'Cần hỗ trợ chăm sóc người già tại nhà', '2025-06-10', 'Urgent', 'Cần tình nguyện viên ghé thăm và chăm sóc y tế cơ bản cho người già neo đơn.', 'nguoicanhotro1', 'Closed', '0933333333', '789 Needy Rd, Da Nang'),
(2, 'Cần dạy kèm tiếng Anh cho trẻ em khó khăn', '2025-06-20', 'Normal', 'Cần tình nguyện viên dạy kèm tiếng Anh online hoặc trực tiếp cho nhóm 5 trẻ em.', 'nguoicanhotro2', 'Approved', '0944444444', '101 Support Ln, Hue'),
(3, 'Cần sửa chữa lại mái nhà dột', '2025-07-01', 'High', 'Mái nhà đã xuống cấp, cần một đội sửa chữa giúp đỡ.', 'nguoicanhotro1', 'Closed', '0933333333', '789 Needy Rd, Da Nang'),
(4, 'Cần hỗ trợ mua thuốc định kỳ', '2025-07-05', 'Urgent', 'Do đi lại khó khăn, cần người đi mua thuốc giúp hàng tuần.', 'nguoicanhotro2', 'Rejected', '0944444444', '101 Support Ln, Hue'),
(5, 'Cần tình nguyện viên đọc sách cho người khiếm thị', '2025-09-01', 'Low', 'Cần người có giọng đọc tốt, đọc sách ghi âm lại cho người khiếm thị.', 'nguoicanhotro1', 'Pending', '0933333333', '789 Needy Rd, Da Nang');

-- 8. Events entries
-- Event 1: Linked to Request #1, Done.
-- Event 2: Standalone event, Upcoming.
-- Event 3: Standalone event, Pending Admin approval.
-- Event 4: Standalone event, Cancelled.
-- Event 5: Org created this for Request #2, but it's still Pending Admin approval. So Request #2 is NOT closed yet.
-- Event 6: Org created this for Request #3, and Admin approved it. So Request #3 is now Closed.
INSERT INTO Events (eventId, title, maxParticipantNumber, startDate, endDate, emergencyLevel, description, organizer, requestId, status) VALUES
(1, 'Chăm sóc sức khỏe người già', 5, '2025-06-25', '2025-06-28', 'Urgent', 'Chăm sóc và hỗ trợ y tế cho người già theo yêu cầu #1.', 'tochuc2', 1, 'Done'),
(2, 'Hiến máu nhân đạo Mùa Hè Xanh', 50, '2025-08-01', '2025-08-01', 'High', 'Chương trình hiến máu cứu người tại trung tâm thành phố.', 'tochuc2', NULL, 'Upcoming'),
(3, 'Quyên góp sách vở cho năm học mới', 20, '2025-07-10', '2025-07-15', 'Normal', 'Quyên góp và phân loại sách vở, đồ dùng học tập cho trẻ em nghèo.', 'tochuc1', NULL, 'Pending'),
(4, 'Dọn dẹp bãi biển Cửa Đại', 30, '2025-07-20', '2025-07-20', 'Low', 'Dọn dẹp rác thải tại bãi biển Cửa Đại để bảo vệ môi trường.', 'tochuc1', NULL, 'Cancelled'),
(5, 'Dạy kèm tiếng Anh tình nguyện', 5, '2025-08-05', '2025-08-25', 'Normal', 'Dạy kèm tiếng Anh cho trẻ em khó khăn theo yêu cầu #2.', 'tochuc1', 2, 'Pending'),
(6, 'Sửa chữa nhà cửa giúp dân', 10, '2025-07-15', '2025-07-17', 'High', 'Sửa lại mái nhà cho hộ dân theo yêu cầu #3', 'tochuc2', 3, 'Upcoming');

-- 9. EventSkills linking
INSERT INTO EventSkills (eventID, skillId) VALUES
(1, 1), (1, 2), -- Event 1 needs Communication and First Aid
(2, 2), -- Event 2 needs First Aid
(3, 1), -- Event 3 needs Communication
(4, 1), -- Event 4 needs Communication
(5, 3), -- Event 5 needs Education
(6, 5); -- Event 6 needs Driving

-- 10. EventParticipants entries (consistent with event status)
-- Volunteer tnv1 participated in the 'Done' event 1.
-- Volunteer tnv2 also participated in the 'Done' event 1.
-- Volunteer tnv1 is registered for the 'Upcoming' event 2.
INSERT INTO EventParticipants (eventId, username, hoursParticipated, ratingByOrg) VALUES
(1, 'tnv1', 16, 8),
(1, 'tnv2', 16, 9),
(2, 'tnv1', NULL, NULL);

-- 11. Notification entries (consistent with EventParticipants)
-- Notifications for event 1 are 'Registered' because the volunteers are in the participants list.
-- Notification for tnv1 for event 2 is 'Registered'.
-- Notification for tnv2 for event 2 is still 'Pending' as an example of a pending request.
-- Notification for tnv1 for event 3 (a pending event) is 'Pending'.
-- Notification for tnv1 for event 4 (a cancelled event) is 'Cancelled'.
-- Notification for tnv2 (who has teaching skill) for event 5 (a pending event) is 'Pending'.
-- Notification for tnv1 (who has driving skill) for event 6 (an upcoming event) is 'Registered'.
INSERT INTO Notification (notificationId, eventId, username, acceptStatus) VALUES
(1, 1, 'tnv1', 'Registered'),
(2, 1, 'tnv2', 'Registered'),
(3, 2, 'tnv1', 'Registered'),
(4, 2, 'tnv2', 'Pending'),
(5, 3, 'tnv1', 'Pending'),
(6, 4, 'tnv1', 'Cancelled'),
(7, 5, 'tnv2', 'Pending'),
(8, 6, 'tnv1', 'Registered');

-- Sample data for Report
INSERT INTO Report (reportId, eventId, reportDate, progress, note)
VALUES
(1, 1, '2025-06-18', 50, 'Đã hoàn thành một nửa chương trình'),
(2, 1, '2025-06-20', 100, 'Đã hoàn thành toàn bộ chương trình');

-- Sample data for FinalReport
INSERT INTO FinalReport (reportId)
VALUES (2);