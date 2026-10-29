package com.infotech.lang.application.data.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.TableGenerator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.Type;

@Entity
public class Teacher /*extends AbstractEntity*/ {

	@Id
	@TableGenerator(name = "Teacher_Gen", table = "ID_GEN", pkColumnName = "GEN_NAME", valueColumnName = "GEN_VAL", pkColumnValue = "Teacher_Gen", initialValue = 102, allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.TABLE, generator = "Teacher_Gen")
	
    private long teacherId ; 
	
	@NotEmpty
	private String firstName = "";
	
	@NotEmpty
	private String lastName = "";
	
	@Email
	@NotEmpty
	private String email = "";

	// Login userName of the teacher account this roster row represents.
	// Links students (User.teacher) to a teacher login for the progress dashboard.
	@Column(name = "user_name", length = 255)
	private String userName;

	// Short code this teacher shares with students so self-registration can
	// auto-link them to this teacher (see UserRegistrationService). Unique when set.
	@Column(name = "student_code", length = 20)
	private String studentCode;

	// Curriculum this teacher (and transitively their students, via User.teacher)
	// follows. NULL = the language's default (house) curriculum. See
	// CurriculumService.resolveCurriculumFor and docs/curriculum-content-model-plan.md.
	@Column(name = "curriculum_id")
	private Long curriculumId;

	@NotNull
	@ManyToOne
	private Status status;

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getStudentCode() {
		return studentCode;
	}

	public void setStudentCode(String studentCode) {
		this.studentCode = studentCode;
	}

	public Long getCurriculumId() {
		return curriculumId;
	}

	public void setCurriculumId(Long curriculumId) {
		this.curriculumId = curriculumId;
	}


	public long getTeacherId() {
		return teacherId;
	}

	public void setTeacherId(int teacherId) {
		this.teacherId = teacherId;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}


  
}

