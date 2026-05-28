package mashup.spring16.tamagotchi.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "tamagotchi")
public class Tamagotchi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "member_id", unique = true, nullable = false)
    private Member member;

    @Column(nullable = false)
    private String character;

    @Column(nullable = false)
    private String name;

    protected Tamagotchi() {}

    public Tamagotchi(Member member, String character, String name) {
        this.member = member;
        this.character = character;
        this.name = name;
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public String getCharacter() { return character; }
    public String getName() { return name; }
}
