package task.agile.taskagile.domain.application.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import task.agile.taskagile.domain.application.UserService;
import task.agile.taskagile.domain.application.commands.RegistrationCommand;
import task.agile.taskagile.domain.common.event.DomainEventPublisher;
import task.agile.taskagile.domain.common.mail.MailManager;
import task.agile.taskagile.domain.common.mail.MessageVariable;
import task.agile.taskagile.domain.model.user.RegistrationManagement;
import task.agile.taskagile.domain.model.user.User;
import task.agile.taskagile.domain.model.user.UserRepository;
import task.agile.taskagile.domain.model.user.events.UserRegisteredEvent;
import task.agile.taskagile.domain.model.user.exceptions.RegistrationException;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final RegistrationManagement registrationManagement;
  private final DomainEventPublisher domainEventPublisher;
  private final MailManager mailManager;
  private final UserRepository userRepository;

  @Override
  public void register(RegistrationCommand command) throws RegistrationException {
    Assert.notNull(command, "Parameter command must not be null");
    // 사용자 등록
    // 실제 사용자 등록은 도메인 이벤트에서
    User newUser = registrationManagement.register(
      command.getUsername(),
      command.getEmailAddress(),
      command.getPassword());

    // 메일 보내기
    sendWelcomeMessage(newUser);

    // 이벤트 발생
    domainEventPublisher.publish(new UserRegisteredEvent(newUser));
  }

  private void sendWelcomeMessage(User user) {
    mailManager.send(
      user.getEmailAddress(),
      "Welcome to task agile",
      "welcome.ftl",
      MessageVariable.from("user", user)
    );
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    if (StringUtils.isEmpty(username)) {
      throw new UsernameNotFoundException("No user found");
    }

    Optional<User> user;
    if (username.contains("@")) {
      user = userRepository.findByEmailAddress(username);
    } else {
      user = userRepository.findByUsername(username);
    }

    if (!user.isPresent()) {
      throw new UsernameNotFoundException("No user found by '" + username + "'");
    }

    return null;
  }
}
